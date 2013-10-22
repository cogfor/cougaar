#include <jni.h>
#include "org_cougaar_core_node_service_jvmdump_JniStackDump.h"
#ifdef _WIN32
#include <windows.h>
#else
#include <signal.h>
#include <unistd.h>
#include <sys/types.h>
#endif /* _WIN32 */

/* see JvmStackDumpServiceComponent for copywrite & docs */

JNIEXPORT jboolean JNICALL Java_org_cougaar_core_node_service_jvmdump_JniStackDump_jvmdump
  (JNIEnv * env, jclass clz) {
#ifdef _WIN32
  if (GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, 0) == 0) {
    /* optionally examine the GetLastError() */
    return JNI_FALSE;
  }
  return JNI_TRUE;
#else
  pid_t jvmpid;
  int killret;
  /* get the jvm pid -- this seems to work */
  jvmpid = getpgrp();
  if (jvmpid <= 0) {
    return JNI_FALSE;
  }
  killret = kill(jvmpid, SIGQUIT);
  if (killret != 0) {
    return JNI_FALSE;
  }
  return JNI_TRUE;
#endif
}
