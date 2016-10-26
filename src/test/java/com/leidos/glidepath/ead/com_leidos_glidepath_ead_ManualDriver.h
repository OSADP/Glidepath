/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_leidos_glidepath_ead_ManualDriver */

#ifndef _Included_com_leidos_glidepath_ead_ManualDriver
#define _Included_com_leidos_glidepath_ead_ManualDriver
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_leidos_glidepath_ead_ManualDriver
 * Method:    initialize
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_ead_ManualDriver_initialize
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_leidos_glidepath_ead_ManualDriver
 * Method:    set_constraints
 * Signature: (DDDD)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_ead_ManualDriver_set_1constraints
  (JNIEnv *, jobject, jdouble, jdouble, jdouble, jdouble);

/*
 * Class:     com_leidos_glidepath_ead_ManualDriver
 * Method:    set_state
 * Signature: (DDDIDD)V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_ead_ManualDriver_set_1state
  (JNIEnv *, jobject, jdouble, jdouble, jdouble, jint, jdouble, jdouble);

/*
 * Class:     com_leidos_glidepath_ead_ManualDriver
 * Method:    get_target_speed
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_com_leidos_glidepath_ead_ManualDriver_get_1target_1speed
  (JNIEnv *, jobject);

/*
 * Class:     com_leidos_glidepath_ead_ManualDriver
 * Method:    close_ead_logs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_leidos_glidepath_ead_ManualDriver_close_1ead_1logs
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
