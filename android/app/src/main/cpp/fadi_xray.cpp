#include <jni.h>

extern "C" {
    char* CGoInvoke(char* requestJSON);
    void CGoFree(char* value);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_fadi_vpn_XrayRunner_nativeInvoke(
        JNIEnv* env,
        jobject thiz,
        jstring request) {

    if (request == nullptr)
        return nullptr;

    const char* input =
        env->GetStringUTFChars(request, nullptr);

    if (input == nullptr)
        return nullptr;

    char* response =
        CGoInvoke(const_cast<char*>(input));

    env->ReleaseStringUTFChars(request, input);

    if (response == nullptr)
        return nullptr;

    jstring result =
        env->NewStringUTF(response);

    CGoFree(response);

    return result;
}
