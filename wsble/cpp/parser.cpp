
#include <jni.h>
#include <string>
#include <random>
#include <sstream>
#include <android/log.h>
#include "parser/parser.hpp"


static const std::string &generateUUID() {
    static std::random_device              rd;
    static std::mt19937                    gen(rd());
    static std::uniform_int_distribution<> dis(0, 15);
    static std::uniform_int_distribution<> dis2(8, 11);

    std::stringstream ss;
    int i;
    ss << std::hex;
    for (i = 0; i < 8; i++) {
        ss << dis(gen);
    }
    ss << "-";
    for (i = 0; i < 4; i++) {
        ss << dis(gen);
    }
    ss << "-4";
    for (i = 0; i < 3; i++) {
        ss << dis(gen);
    }
    ss << "-";
    ss << dis2(gen);
    for (i = 0; i < 3; i++) {
        ss << dis(gen);
    }
    ss << "-";
    for (i = 0; i < 12; i++) {
        ss << dis(gen);
    };
    return ss.str();
}



static unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array)
{
    int len = env->GetArrayLength(array);
    unsigned char *buf = new unsigned char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}


static void print_raw_data(const unsigned char *value, int n) {
    std::string str;
    char c[16];

    for (int i = 0; i < n; ++i) {
        sprintf(c, "0x%02x", value[i]);
        str += c;
    }

    __android_log_print(ANDROID_LOG_INFO, "BLE", "raw data >> %s", str.c_str());
}


extern "C" JNIEXPORT void JNICALL
Java_xyz_wonder_1wonder_android_wondersensekit_WSBLE_initParser(JNIEnv *, jobject,
                                          jint accFsr, jint gyroFsr,
                                          jfloat magXcoef, jfloat magYcoef, jfloat magZcoef)
{
    auto property = sensor_property {
            static_cast<std::uint8_t>(accFsr),
            static_cast<std::uint16_t>(gyroFsr),
            magXcoef,
            magYcoef,
            magZcoef
    };
    init_parser(property);
}



constexpr int PACKET_SIZE = 4;
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_xyz_wonder_1wonder_android_wondersensekit_WSBLE_native_1parse(JNIEnv *env, jobject thiz, jbyteArray value) {
    jclass dataClass = env->FindClass("xyz/wonder_wonder/android/wondersensekit/WSBLEData");
    jobjectArray ret = env->NewObjectArray(PACKET_SIZE, dataClass, {});

    jmethodID dataClassSetterMethod = env->GetMethodID(dataClass, "<init>",
                                                       "(Ljava/lang/String;DDDDDDDDDDDD)V");

    auto byte_c_value = as_unsigned_char_array(env, value);
    print_raw_data(byte_c_value, 96);

    auto sensorData = parse(byte_c_value);
    for (int i = 0; i < PACKET_SIZE; ++i) {
        jobject dataClassObject;

        const std::string str = generateUUID();
        jstring id = env->NewString(reinterpret_cast<const jchar *>(str.c_str()), 40);

        dataClassObject = env->NewObject(dataClass, dataClassSetterMethod,
                                         id,
                                         sensorData[i].ax, sensorData[i].ay, sensorData[i].az,
                                         sensorData[i].gx, sensorData[i].gy, sensorData[i].gz,
                                         sensorData[i].mx, sensorData[i].my, sensorData[i].mz,
                                         sensorData[i].temperature, sensorData[i].battery, sensorData[i].airPressure
        );

        env->SetObjectArrayElement(ret, i, dataClassObject);
    }

    delete byte_c_value;
    free(sensorData);
    __android_log_print(ANDROID_LOG_INFO, "BLE", "finish a native code for parse.");

    return ret;
}


extern "C"
JNIEXPORT void JNICALL
Java_xyz_wonder_1wonder_android_wondersensekit_WSBLE_testParser(JNIEnv *env, jobject thiz,
                                                                jbyteArray value) {
    auto byte_c_value = as_unsigned_char_array(env, value);
}