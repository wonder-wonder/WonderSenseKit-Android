#include "parser.hpp"
#include <cstdint>
#include <android/log.h>

// define
struct data_packet_t {
  short ax, ay, az;   // 6 bytes
  short gx, gy, gz;   // 6 bytes
  short mx, my, mz;   // 6 bytes
  short temperature;  // 2 bytes
  short battery;      // 2 bytes
  short airPressure;  // 2 bytes //total size:24 bytes
}; // 24 * 4 = 96


// values
sensor_property m_property;


// functions
void init_parser(sensor_property data)
{
  m_property = data;
}


constexpr int packet_size = 4;
constexpr double coef_s2d = 32768.0;
constexpr double coef_mag = 0.15;
sensor_data *parse(void *data)
{
  data_packet_t *data_packets;
  data_packets = (data_packet_t *)data;

  sensor_data *ret;
  ret = (sensor_data *)malloc(sizeof(sensor_data) * packet_size);

  for (int i = 0; i < packet_size; ++i) {
    ret[i].ax = ((data_packets[i].ax * m_property.accFsr) / coef_s2d);
    ret[i].ay = ((data_packets[i].ay * m_property.accFsr) / coef_s2d);
    ret[i].az = ((data_packets[i].az * m_property.accFsr) / coef_s2d);

    ret[i].gx = (data_packets[i].gx * m_property.gyroFsr / coef_s2d);
    ret[i].gy = (data_packets[i].gy * m_property.gyroFsr / coef_s2d);
    ret[i].gz = (data_packets[i].gz * m_property.gyroFsr / coef_s2d);

    ret[i].mx = (data_packets[i].mx * coef_mag * m_property.magXcoef);
    ret[i].my = (data_packets[i].my * coef_mag * m_property.magYcoef);
    ret[i].mz = (data_packets[i].mz * coef_mag * m_property.magZcoef);

    ret[i].battery     = data_packets[i].battery / 4096.0 * 3.3 * 4.9;
    ret[i].airPressure = data_packets[i].airPressure / 4096.0 * 3.3;
    ret[i].temperature = data_packets[i].temperature / 333.87 + 21.0;

    __android_log_print(ANDROID_LOG_INFO, "BLE", "raw ax >> %d: %hi", i, data_packets[i].ax);
    __android_log_print(ANDROID_LOG_INFO, "BLE", ">> %d: %lf", i, ret[i].ax);
  }

  return ret;
}
