/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_leidos_glidepath_can_CanSocket */

#ifndef _Included_com_leidos_glidepath_can_CanSocket
#define _Included_com_leidos_glidepath_can_CanSocket
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _open
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_leidos_glidepath_can_CanSocket__1open
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_can_CanSocket__1close
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _send
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_leidos_glidepath_can_CanSocket__1send
  (JNIEnv *, jobject, jint, jbyteArray);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _recv
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_leidos_glidepath_can_CanSocket__1recv
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _setSocketTimeout
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_can_CanSocket__1setSocketTimeout
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     com_leidos_glidepath_can_CanSocket
 * Method:    _poll
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_leidos_glidepath_can_CanSocket__1poll
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif