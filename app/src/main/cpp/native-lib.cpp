#include <jni.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cerrno>

#define SERVER_PATH "/data/data/org.coderus.aliendalvikcontrol/.aliendalvik-control-socket"

extern "C" JNIEXPORT jstring JNICALL
Java_org_coderus_aliendalvikcontrol_Native_reply(JNIEnv* env, jobject /*thiz*/, jstring data) {
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
