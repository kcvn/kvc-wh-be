package com.kcvn.spm.sample.service

import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.sample.dto.FileDto
import jakarta.servlet.http.HttpServletResponse
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.fileSize


@Service
class ImportExportService(private val jobLauncher: JobLauncher) {
    fun import(job: Job, multipartFile: MultipartFile, errorFileName: String): ResponseEntity<*> {
        val tempFolderPath = Files.createTempDirectory(Paths.get("/"), "temp")

        val filepath: Path = Files.createTempFile(
            tempFolderPath,
            null,
            multipartFile.originalFilename
        )
        Files.newOutputStream(filepath).use { os -> os.write(multipartFile.bytes) }

        val errorFilePath = Files.createTempFile(
            tempFolderPath,
            null,
            errorFileName
        )

        try {
            //Launch the Batch Job
            val jobExecution = jobLauncher.run(
                job,
                JobParametersBuilder()
                    .addString("fullPathFileName", filepath.toString())
                    .addString("outputFile", errorFilePath.toString())
                    .toJobParameters()
            )

            return if (jobExecution.stepExecutions.any { it.exitStatus.exitCode.equals("COMPLETED WITH SKIPS") }) {
//                val resource = InputStreamResource(FileInputStream(errorFilePath.toFile()))//ByteArrayResource(Files.readAllBytes(errorFilePath))
//                val headers = HttpHeaders()
//                headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
//                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=error.csv")
//                headers.add("Import-Message", "There are records with errors")
                ResponseEntity.ok()
//                    .headers(headers)
//                    .contentLength(errorFilePath.fileSize())
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(MessageResponse(
                        "there are error records!",
                        FileDto(
                            errorFileName,
                            if (errorFileName.contains(".xlsx")) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "text/csv",
                            Files.readAllBytes(errorFilePath)
                        )
                    ))
            } else {
                ResponseEntity.ok().body(MessageResponse(jobExecution.status.name))
            }
        } finally {
            filepath.toFile().delete()
            errorFilePath.toFile().delete()
            tempFolderPath.toFile().delete()
        }

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