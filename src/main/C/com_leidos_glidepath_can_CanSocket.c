/* SocketCAN Support Module for Glidepath
 * JNI module to incorporate Linux SocketCAN interface to Java object
 * Kyle Rush <kyle.rush@leidos.com> 2015
 */

#include <linux/can.h>
#include <linux/can/raw.h>
#include <linux/can/error.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include "com_leidos_glidepath_can_CanSocket.h"

void throwIOException(char*);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _open
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_leidos_glidepath_can_CanSocket__1open
(JNIEnv * env, jobject self, jstring device) {
	struct ifreq ifr;
	struct sockaddr_can addr;
 
	// Open the socket file descriptor
	int sockfd = socket(PF_CAN, SOCK_RAW, CAN_RAW);
	if (sockfd < 0) {
		throwIOException(env, "Error opening CAN socket.");
	}

	// Set the netdevice
	const char * device_name = (env*)->GetStringUTFChars(env, device, 0);
	strcpy(ifr.ifr_name, device_name);
	if (ioctl(sfd, SIOCGIFINDEX, &ifr) < 0) {
		throwIOException(env, "SIOCGIFINDEX error opening CAN socket.");
	}

	// Enable address reuse
	int flag = 1;
	if (setsockopt(sfd, SOL_SOCKET, SO_REUSEADDR, &flag, sizeof(reuse)) < 0) {
		throwIOException(env, "Unable to allow address reuse for CAN socket.");
	}

	// Bind the socket
	addr.can_family = AF_CAN;
	addr.can_ifindex = ifr.ifr_ifindex;
	if (bind(sockfd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
		throwIOException(env, "Unable to bind CAN socket.");
	}
	
	// Free JNI resources
	(env*)->ReleaseStringUTFChars(env, device, device_name);

	return (jint) sockfd;
}

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_can_CanSocket__1close
(JNIEnv *env, jobject self, jint sockfd) {
	close((int) sockfd);
}

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _send
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_leidos_glidepath_can_CanSocket__1send
(JNIEnv *env, jobject self, jint sockfd, jbyteArray data) {
	int dataLen = (env*)->GetArrayLength(data);
	unsigned char* buf = new unsigned char[dataLen];
	(env*)->GetByteArrayRegion(env, array, 0, dataLen, buf);

	
	int bytes = write((int)sockfd, buf, sizeof(struct can_frame));
	if (bytes < sizeof(dataLen)) {
		throwIOException(env, "Failed to send proper number of bytes over CAN socket.");
	}

	return (jint) bytes;
}

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _recv
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_leidos_glidepath_can_CanSocket__1recv
(JNIEnv *env, jobject self, jint sockfd) {
	unsigned char buf[13];
	int bytes = recv((int)sockfd, buf, sizeof(buf), 0);

	if (bytes <= 0) {
		throwIOException(env, "Error reading data from CAN socket.");
	}

	// Convert buf into jbyteArray
	jbyteArray out = (env*)->NewByteArray(env, bytes);
	(env*)->SetByteArrayRegion(env, array, 0, len, buf);
	return out;
}

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _setSocketTimeout
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_can_CanSocket__1setSocketTimeout
(JNIEnv *env, jobject self, jint sockfd, jint millis) {
	// Construct timeout struct
	struct timeval timeout;
	timeout.tv_sec = (int) millis / 1000;
	timeout.tv_usec = ((int) millis % 1000) * 1000;

	// Set timeout
	if (setsockopt((int) sockfd, SOL_SOCKET, SO_RCVTIMEO,
				   (char *) &timeout, sizeof(timeout)) < 0) {
		throwIOException(env, "Unable to set socket timeout for CAN socket.");
	}

	if (setsockopt((int) sockfd, SOL_SOCKET, SO_SNDTIMEO,
				   (char *) &timeout, sizeof(timeout)) < 0) {
		throwIOException(env, "Unable to set socket timeout for CAN socket.");
	}
}

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _poll
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_leidos_glidepath_can_CanSocket__1poll
(JNIEnv *env, jobject self, jint sockfd) {
	// File descriptor set 
	fd_set fds;
	FD_ZERO(&fds);
	FD_SET((int) sockfd, &fds);

	// Select timeout, zero because we just want to check current state
	struct timeval tv;
	tv.tv_sec = 0;
	tv.tv_usec = 0;

	if (select((int) sockfd + 1, &fds, NULL, NULL, &tv)value < 0) {
		throwIOException(env, "Error calling select() on CAN socket.");
	} else {
		return FD_ISSET((int) sockfd, &fds);
	}
}

void throwIOException(JNIEnv *env, char* message) {
	jclass exception = (env*)->FindClass("java/io/IOException");
	(env*)->ThrowNew(exception, message);
}

