#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>

#define SERVER_PATH "/data/data/org.coderus.aliendalvikcontrol/.aliendalvik-control-socket"

static const char *className = "org/coderus/aliendalvikcontrol/Native";

static jstring reply(JNIEnv* env, jobject /*thiz*/, jstring data) {
    char result[1024];

    int sock;
    sock = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock == -1) {
        sprintf(result, "Bind error (%s)", strerror(errno));
        return env->NewStringUTF(result);
    }

    struct sockaddr_un server;
    server.sun_family = AF_UNIX;
    strcpy(server.sun_path, SERVER_PATH);

    if (connect(sock, (struct sockaddr *)&server, sizeof(server)) < 0) {
        sprintf(result, "Connect error (%s)", strerror(errno));
        return env->NewStringUTF(result);
    }

    const char *nativeData = env->GetStringUTFChars(data, 0);

    if (send(sock, nativeData, strlen(nativeData), 0) < 0) {
        sprintf(result, "Send error (%s)", strerror(errno));
    } else {
        sprintf(result, "No errors");
    }

    env->ReleaseStringUTFChars(data, nativeData);

    close(sock);

    return env->NewStringUTF(result);
}

static JNINativeMethod methods[] = {
        {"reply", "(Ljava/lang/String;)Ljava/lang/String;", (void*)reply },
};

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        printf("jni version mismatch\n");
        return -1;
    }

    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        printf("could not locate clazz\n");
        return -1;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        printf("cant register native methods\n");
        return -1;
    }

    return JNI_VERSION_1_6;
}
