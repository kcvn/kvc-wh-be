package com.kcvn.spm.common.helper

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Render template Thymeleaf trong resources/templates thành file PDF.
 * Font family trong CSS của template phải là 'NotoSans' để hiển thị được tiếng Việt.
 */
@Component
class PdfHelper(private val templateEngine: TemplateEngine) {

    fun render(templateName: String, variables: Map<String, Any?>): ByteArray {
        val html = templateEngine.process(templateName, Context(Locale.getDefault(), variables))
        val baseUri = javaClass.getResource("/templates/")?.toExternalForm()

        return ByteArrayOutputStream().use { out ->
            PdfRendererBuilder().apply {
                useFastMode()
                withHtmlContent(html, baseUri)
                registerFonts(this)
                toStream(out)
            }.run()
            out.toByteArray()
        }
    }

    private fun registerFonts(builder: PdfRendererBuilder) {
        listOf(
            "fonts/NotoSans-Regular.ttf" to 400,
            "fonts/NotoSans-Bold.ttf" to 700,
        ).forEach { (path, weight) ->
            builder.useFont(
                { javaClass.classLoader.getResourceAsStream(path) },
                FONT_FAMILY,
                weight,
                BaseRendererBuilder.FontStyle.NORMAL,
                true
            )
        }
    }

    companion object {
        private const val FONT_FAMILY = "NotoSans"
    }
}
