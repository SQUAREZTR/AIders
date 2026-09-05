package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class QuickPromptItem(
    val label: String,
    val prompt: String,
    val icon: String
)

@Composable
fun QuickPromptChips(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePrompts = listOf(
        QuickPromptItem(
            label = "Türev Ekstremum Çözümü",
            prompt = "f(x) = 2x^3 - 6x^2 + 5 fonksiyonunun yerel ekstremum değerlerini ve teğet püf noktalarını adım adım çöz.",
            icon = "📝"
        ),
        QuickPromptItem(
            label = "Trigonometri Çember Kodlaması",
            prompt = "Birim çemberde açılara göre trigonometrik fonksiyonların işaretlerini ve sınavda isim değiştirme kurallarını taktiklerle anlat.",
            icon = "🎓"
        ),
        QuickPromptItem(
            label = "Biyoloji Fotosentez Tuzağı",
            prompt = "Fotosentezin ışığa bağımlı ve ışıktan bağımsız tepkimelerindeki soru tuzaklarını ve ÖSYM kalıplarını özetle.",
            icon = "⚡"
        ),
        QuickPromptItem(
            label = "Sınavda Zaman Yönetimi",
            prompt = "Deneme sınavlarında süreyi yetiştiremiyorum, turlama tekniğini ve soruyla inatlaşmama taktiğini anlatır mısın?",
            icon = "🎯"
        ),
        QuickPromptItem(
            label = "Fizik Basit Harmonik Hareket",
            prompt = "Basit harmonik harekette maksimum hız ve ivme noktalarının formül taktiklerini ve grafik tuzaklarını açıkla.",
            icon = "💡"
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Hızlı Soru & Konu Örnekleri",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(samplePrompts) { item ->
                SuggestionChip(
                    onClick = { onSelectPrompt(item.prompt) },
                    label = {
                        Text(
                            text = "${item.icon} ${item.label}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        enabled = true
                    ),
                    modifier = Modifier.testTag("quick_chip_${item.label.replace(" ", "_")}")
                )
            }
        }
    }
}
