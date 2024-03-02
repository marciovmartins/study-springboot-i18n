package dev.martins.marcio.studies.springbooti18n

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URI
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream

@WebMvcTest(PersonController::class)
class PersonControllerIT(
    @Autowired private val mockMvc: MockMvc
) {
    @Value("\${app.api-errors.baseUrl}")
    private var apiErrorsBaseUrl: String = ""

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(InvalidPersonArgumentsProvider::class)
    fun `should return proper error payload when creating a person with the missing name property`(
        testDescription: String,
        personPayload: String,
        locale: Locale,
        title: String,
        expectedError: InvalidPersonArgumentsProvider.ExpectedError,
    ) {
        // given
        val personId = UUID.randomUUID().toString()

        val apiErrorsBaseURI = URI.create(apiErrorsBaseUrl)
        val relativeTypeURI = URI.create("/api-docs/errors/v1/validation-failure")

        // when
        val resultActions = mockMvc.perform(
            put("/people/$personId")
                .contentType(MediaType.APPLICATION_JSON)
                .locale(locale)
                .content(personPayload)
        )

        // then
        resultActions.andExpectAll(
            status().isBadRequest,
            jsonPath("$.type", equalTo(apiErrorsBaseURI.resolve(relativeTypeURI).toString())),
            jsonPath("$.title", equalTo(title)),
            jsonPath("$.status", equalTo(400)),
            jsonPath("$.errors[0].detail", equalTo(expectedError.detail)),
            jsonPath("$.errors[0].errorCode", equalTo(expectedError.errorCode)),
            jsonPath("$.errors[0].pointer", equalTo(expectedError.pointer)),
        )
    }

    object InvalidPersonArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
            argument(
                testDescription = "missing name property and locale 'en'",
                personPayload = """
                    {
                        "age": 17
                    }
                """.trimIndent(),
                locale = Locale.of("en"),
                title = "Validation failure",
                expectedError = ExpectedError(
                    detail = "must not be null",
                    errorCode = "00001",
                    pointer = "name",
                )
            ),
            argument(
                testDescription = "missing name property and locale 'pt-br'",
                personPayload = """
                    {
                        "age": 17
                    }
                """.trimIndent(),
                locale = Locale.of("pt_BR"),
                title = "Falha de validação",
                expectedError = ExpectedError(
                    detail = "não deve ser nulo",
                    errorCode = "00001",
                    pointer = "name",
                )
            ),
        )

        private fun argument(
            testDescription: String,
            personPayload: String,
            locale: Locale,
            title: String,
            expectedError: ExpectedError,
        ) = Arguments.of(testDescription, personPayload, locale, title, expectedError)

        data class ExpectedError(
            val detail: String,
            val errorCode: String,
            val pointer: String,
        )
    }
}