import re

with open('app/src/main/java/com/example/api/GeminiApi.kt', 'r') as f:
    text = f.read()

old_func = """    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse"""

new_func = """    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse"""

text = text.replace(old_func, new_func)

with open('app/src/main/java/com/example/api/GeminiApi.kt', 'w') as f:
    f.write(text)
print("Patched GeminiApi.kt")
