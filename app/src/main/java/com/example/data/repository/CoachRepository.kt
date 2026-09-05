package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.audio.TextToSpeechManager
import com.example.data.local.CoachHistoryDao
import com.example.data.model.CoachHistoryEntity
import com.example.data.model.CoachResponse
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiInlineData
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoachRepository(
    private val context: Context,
    private val historyDao: CoachHistoryDao,
    private val ttsManager: TextToSpeechManager
) {
    private val TAG = "CoachRepository"

    val allHistory: Flow<List<CoachHistoryEntity>> = historyDao.getAllHistory()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val coachResponseAdapter = moshi.adapter(CoachResponse::class.java)

    companion object {
        const val SYSTEM_PROMPT = """Sen, öğrencilere birebir özel ders veren, dinamik, motive edici ve son derece tecrübeli bir öğretmen ve sınav koçusun. Görevin; sana gönderilen görselleri (ders notları, kitap sayfaları, soru fotoğrafları) veya metinleri analiz ederek eksiksiz bir öğrenme deneyimi sunmaktır.

ÇALIŞMA PRENSİBİ VE KARAR MEKANİZMASI:
Gelen her isteği ilk olarak analiz et ve içeriğin türünü tespit et. Yanıtını aşağıdaki kurallara göre tek bir JSON formatında döndür:

1. EĞER GÖNDERİLEN GÖRSEL VEYA METİN BİR "SORU" İSE (Öğrenci sadece bir soru fotoğrafı veya soru metni göndermiş olsa bile):
- Gönderilen içeriğin bir soru olduğunu ("QUESTION_SOLUTION") tespit et.
- Soruyu adım adım, mantığını ve formüllerini açıklayarak detaylıca çöz ve doğru cevabı belirt.
- Çözümün püf noktalarını, kullanılan formülleri ve yapılan yaygın hataları/tuzakları vurgula.
- "audio_script" alanında: Sorunun çözümünü sanki masada öğrencinin yanında oturan samimi bir özel ders öğretmeni sesli anlatıyormuş gibi adım adım, tane tane ve motive edici bir konuşma diliyle yaz ("Selam dostum, bu soruda ilk olarak...", "Buradaki tuzağa dikkat et, çünkü...").
- "type" alanını "QUESTION_SOLUTION" yap.

2. EĞER GÖNDERİLEN GÖRSEL VEYA METİN BİR "DERS KONUSU / DERS NOTU" İSE:
- Düz özet çıkarma. Tıpkı bir öğretmenin derste anlattığı gibi dinamik bir anlatım dili kullan.
- Anlatımda sınav taktikleri, akılda tutma kodlamaları (akrostiş, benzetmeler), sınavda soru getirecek kilit noktalar ve soru tuzaklarını dahil et.
- Bu anlatımı, seslendirme motorunun (TTS) akıcı biçimde okuyabileceği doğal bir konuşma metnine ("audio_script") dönüştür.
- "type" alanını "LESSON_TEACHER" yap.

3. EĞER KULLANICI SADECE "SORU SORUYORSA" (Soru-Cevap / Kavram Tekrarı):
- Öğrencinin aklına takılan soruyu samimi, net ve öğretici bir dille yanıtla.
- Yanıtı hem metin olarak sun hem de seslendirmeye uygun kısa bir ses metni ("audio_script") olarak ekle.
- "type" alanını "QA_REPEATER" yap.

ÇIKTI FORMATI (Yalnızca aşağıdaki JSON yapısında yanıt ver):
{
  "type": "QUESTION_SOLUTION" | "LESSON_TEACHER" | "QA_REPEATER",
  "title": "Konu veya Soru Başlığı",
  "display_text": "Kullanıcının ekranda okuyacağı detaylı çözüm, konu anlatımı veya cevap metni",
  "audio_script": "Seslendirme motorunun okuyacağı, konuşma diline uygun, öğretmen edasındaki taktikli sesli anlatım metni",
  "key_takeaways": ["Sınav taktiği 1", "Sınav tuzakları", "Önemli formül/kavram"]
}"""
    }

    suspend fun analyze(
        userPrompt: String,
        imageUri: Uri?
    ): Result<Pair<CoachResponse, CoachHistoryEntity>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY

            // Save local image copy if present
            var savedImagePath: String? = null
            var imageBase64: String? = null

            if (imageUri != null) {
                try {
                    val bitmap = uriToBitmap(imageUri)
                    if (bitmap != null) {
                        savedImagePath = saveBitmapLocally(bitmap)
                        imageBase64 = bitmapToBase64(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image", e)
                }
            }

            val response: CoachResponse = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                callGeminiApi(apiKey, userPrompt, imageBase64)
            } else {
                // Fallback simulation mode with full realistic educational coach data
                generateSimulatedResponse(userPrompt, imageUri != null)
            }

            // Synthesize audio to file for caching
            val audioFile = ttsManager.synthesizeToFile(response.audioScript, "ders")
            val audioPath = audioFile?.absolutePath

            // Save to Room DB
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"))
            val currentDateStr = dateFormat.format(Date())

            val keyTakeawaysJson = try {
                JSONArray(response.keyTakeaways).toString()
            } catch (e: Exception) {
                "[]"
            }

            val historyEntity = CoachHistoryEntity(
                date = currentDateStr,
                title = response.title,
                type = response.type,
                displayText = response.displayText,
                audioScript = response.audioScript,
                audioPath = audioPath,
                keyTakeawaysJson = keyTakeawaysJson,
                imagePath = savedImagePath,
                userPrompt = userPrompt
            )

            val insertedId = historyDao.insert(historyEntity)
            val savedWithId = historyEntity.copy(id = insertedId)

            Result.success(Pair(response, savedWithId))
        } catch (e: Exception) {
            Log.e(TAG, "Error during analysis", e)
            Result.failure(e)
        }
    }

    private suspend fun callGeminiApi(
        apiKey: String,
        prompt: String,
        imageBase64: String?
    ): CoachResponse {
        val parts = mutableListOf<GeminiPart>()

        val effectivePrompt = if (prompt.isNotBlank()) {
            prompt
        } else if (imageBase64 != null) {
            "Lütfen ekteki görseli incele. Görsel bir test/ödev sorusu ise türünü QUESTION_SOLUTION olarak belirle, soruyu adım adım detaylıca çöz ve özel ders öğretmeni üslubuyla sesli anlatımını da (audio_script) eksiksiz yaz. Eğer bir ders notu veya özet sayfası ise türünü LESSON_TEACHER yap ve taktikli konu anlatımı sun."
        } else {
            "Genel sınav stratejileri ve verimli ders çalışma taktikleri hakkında koçluk yap."
        }

        parts.add(GeminiPart(text = effectivePrompt))

        if (imageBase64 != null) {
            parts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = imageBase64
                    )
                )
            )
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = parts)
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f,
                responseMimeType = "application/json"
            )
        )

        val apiResponse = RetrofitClient.geminiService.generateContent(apiKey, request)
        val rawJsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini'den yanıt alınamadı")

        return parseCoachJson(rawJsonText)
    }

    private fun parseCoachJson(rawText: String): CoachResponse {
        val cleanText = extractJson(rawText)
        return try {
            coachResponseAdapter.fromJson(cleanText) ?: parseManually(cleanText)
        } catch (e: Exception) {
            Log.w(TAG, "Moshi parse failed, attempting manual JSON parse: ${e.message}")
            parseManually(cleanText)
        }
    }

    private fun extractJson(text: String): String {
        var result = text.trim()
        if (result.startsWith("```json")) {
            result = result.removePrefix("```json").trim()
        } else if (result.startsWith("```")) {
            result = result.removePrefix("```").trim()
        }
        if (result.endsWith("```")) {
            result = result.removeSuffix("```").trim()
        }

        val firstBrace = result.indexOf('{')
        val lastBrace = result.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            result = result.substring(firstBrace, lastBrace + 1)
        }
        return result
    }

    private fun parseManually(jsonStr: String): CoachResponse {
        val json = JSONObject(jsonStr)
        val type = json.optString("type", "QUESTION_SOLUTION")
        val title = json.optString("title", "Ders Analizi")
        val displayText = json.optString("display_text", "")
        val audioScript = json.optString("audio_script", "")
        val takeawaysList = mutableListOf<String>()

        val takeawaysArray = json.optJSONArray("key_takeaways")
        if (takeawaysArray != null) {
            for (i in 0 until takeawaysArray.length()) {
                takeawaysList.add(takeawaysArray.getString(i))
            }
        }

        return CoachResponse(
            type = type,
            title = title,
            displayText = displayText,
            audioScript = audioScript,
            keyTakeaways = takeawaysList
        )
    }

    suspend fun deleteHistory(id: Long) = historyDao.deleteById(id)

    suspend fun clearAllHistory() = historyDao.clearAll()

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // scale down for memory efficiency
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun saveBitmapLocally(bitmap: Bitmap): String {
        val imgDir = File(context.cacheDir, "image_cache").apply {
            if (!exists()) mkdirs()
        }
        val file = File(imgDir, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    private fun generateSimulatedResponse(prompt: String, hasImage: Boolean): CoachResponse {
        val lower = prompt.lowercase(Locale.forLanguageTag("tr-TR"))
        return when {
            lower.contains("soru") || hasImage -> {
                CoachResponse(
                    type = "QUESTION_SOLUTION",
                    title = "Türevde Ekstremum ve Teğet Problemi Çözümü",
                    displayText = """### 📌 Soru Analizi & Çözüm Aşamaları:
1. **Verilenler:** Fonksiyonun yerel maksimum noktasında teğetinin eğimi m = 0'dır.
2. **1. Adım:** f(x) = 2x³ - 6x² + 5 fonksiyonunun birinci türevini alalım:
   f'(x) = 6x² - 12x
3. **2. Adım:** Türevi sıfıra eşitleyip kritik noktaları bulalım:
   6x(x - 2) = 0 => x₁ = 0, x₂ = 2
4. **3. Adım (İşaret Tablosu):** 
   - x < 0 için f'(x) > 0 (Artan)
   - 0 < x < 2 için f'(x) < 0 (Azalan)
   - x > 2 için f'(x) > 0 (Artan)
   Dolayısıyla yerel maksimum noktası x = 0, yerel minimum noktası x = 2'dir.
5. **Sonuç:** Maksimum değer f(0) = 5'tir.""",
                    audioScript = "Selam dostum, bu soruda ÖSYM'nin en sevdiği kalıplardan birini görüyoruz. Ekstremum noktalarında türevin sıfır olduğunu asla unutmuyoruz. Birinci türevi aldığımızda 6x kare eksi 12x bulduk. Sıfıra eşitlediğimizde x eşittir 0 ve x eşittir 2 kökleri geldi. İşaret tablosuna dikkat et, artıdan eksiye geçtiğimiz sıfır noktası yerel maksimumdur. Soru bize değeri sorduğu için fonksiyonda yerine koyup beşi işaretliyoruz. Harika bir net!",
                    keyTakeaways = listOf(
                        "Ekstremum noktalarında birinci türev sıfırdır: f'(x) = 0",
                        "Sınav Tuzağı: Noktayı değil 'değeri' sorduğunda f(x)'e dönüp yerine koy!",
                        "İşaret tablosunda köklerin tek katlı veya çift katlı olmasına dikkat et."
                    )
                )
            }
            lower.contains("not") || lower.contains("konu") || lower.contains("anlat") -> {
                CoachResponse(
                    type = "LESSON_TEACHER",
                    title = "Trigonometride İşaretler ve Birim Çember Taktikleri",
                    displayText = """### 🎓 Öğretmen Anlatımı: Birim Çember ve İşaretler
Trigonometride bölgeleri karıştırmamak için sınavda hayat kurtaran kodlamamız:
**"Bütün Sınıf Kara Tahtada Coşar"**

- **1. Bölge (0° - 90°):** **Bütün** fonksiyonlar pozitiftir (+).
- **2. Bölge (90° - 180°):** Sadece **Sinüs** pozitiftir (+). (Sınıf)
- **3. Bölge (180° - 270°):** **Kotanjant ve Tanjant** pozitiftir (+). (Kara Tahta)
- **4. Bölge (270° - 360°):** Sadece **Kosinüs** pozitiftir (+). (Coşar)

### ⚠️ Sınav Tuzağı:
π/2 (90°) ve 3π/2 (270°) açılarında isim değişir! 
(sin <-> cos, tan <-> cot)
Fakat π (180°) ve 2π (360°)'de isim DEĞİŞMEZ!""",
                    audioScript = "Gençler toplanın, trigonometrinin kalbine iniyoruz! Sınavlarda işaret hatası yapıp netlerinizi çöp etmeyin. Kodlamamız neydi? Bütün Sınıf Kara Tahtada Coşar! Birinci bölgede hepsi pozitif, ikincide sadece Sinüs, üçüncüde Tanjant ve Kotanjant, dördüncüde ise sadece Kosinüs pozitif. Ayrıca doksan ve iki yüz yetmiş dereceye değdiğimiz an isim değişir, yüz seksen ve üç yüz altmışta isim sabit kalır. Bu taktiği cebe koy, soru kaçırma!",
                    keyTakeaways = listOf(
                        "Kodlama: Bütün Sınıf Kara Tahtada Coşar (+ işaretler)",
                        "Dikey eksenle (90°, 270°) dönüşümlerde isim değişir: sin -> cos",
                        "Yatay eksenle (180°, 360°) dönüşümlerde sadece işaret kontrol edilir, isim değişmez."
                    )
                )
            }
            else -> {
                CoachResponse(
                    type = "QA_REPEATER",
                    title = "Sınavda Zaman Yönetimi ve Turlama Tekniği",
                    displayText = """### 💬 Koçun Yanıtı:
Sınavda yetiştirememek bilgi eksikliğinden değil, soruyla inatlaşmaktan kaynaklanır.

1. **Turlama Tekniği:** İlk turda sadece 1 dakikadan az süreceğini bildiğin kesin soruları çöz.
2. **İşaretleme:** Çözebileceğin ama zaman alacak sorulara (+) işareti, zor görünenlere (?) koy.
3. **İkinci Tur:** (+) işaretli soruları çöz.
4. **Zihin Tazeleme:** Her 40 dakikada bir 10 saniye gözlerini kapat ve derin nefes al.""",
                    audioScript = "Sevgili öğrencim, sınav bir hız ve psikoloji yarışıdır. Bir soruyla iki dakikadan fazla inatlaşırsan o soru senin netini değil moralini çalar. Turlama tekniğini mutlaka uygula. İlk turda yapabildiklerini topla, ikinci turda orta seviyeleri bitir. Sen bu sınavı kazanacaksın, kendine güven!",
                    keyTakeaways = listOf(
                        "Bir soruyla asla 2 dakikadan fazla inatlaşma!",
                        "Turlama tekniği ile netlerini doğrudan %20 artırabilirsin.",
                        "Denemelerde geliştirdiğin taktiği gerçek sınavda asla değiştirme."
                    )
                )
            }
        }
    }
}
