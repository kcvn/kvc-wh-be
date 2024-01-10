package com.kcvn.spm.sample.service

import jakarta.servlet.http.HttpServletResponse
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.fileSize

@Service
class ImportExportService(private val jobLauncher: JobLauncher) {
    fun import(job: Job, multipartFile: MultipartFile): String {
        val tempFolderPath = Files.createTempDirectory(Paths.get("/"), "temp")

        val filepath: Path = Files.createTempFile(
            tempFolderPath,
            null,
            multipartFile.originalFilename
        )
        Files.newOutputStream(filepath).use { os -> os.write(multipartFile.bytes) }

        //Launch the Batch Job
        val jobExecution = jobLauncher.run(
            job,
            JobParametersBuilder()
                .addString("fullPathFileName", filepath.toString())
                .toJobParameters()
        )

        filepath.toFile().delete()
        tempFolderPath.toFile().delete()
        return jobExecution.status.name
    }

    fun export(response: HttpServletResponse, job: Job, fileName: String): StreamingResponseBody {
        val tempFolderPath = Files.createTempDirectory(Paths.get("/"), "temp")

        val filepathOutput: Path = Files.createTempFile(
            tempFolderPath,
            null,
            fileName
        )

        //Launch the Batch Job
        val jobExecution = jobLauncher.run(
            job, JobParametersBuilder()
                .addString("outputFile", filepathOutput.toString())
                .toJobParameters()
        )

        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${fileName}")
        response.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
        response.setContentLengthLong(filepathOutput.fileSize())
        return StreamingResponseBody { os ->
            Files.copy(filepathOutput, os)
            filepathOutput.toFile().delete()
            tempFolderPath.toFile().delete()
        }
    }
}