#ifndef PARSER_HEADER
#define PARSER_HEADER

#include <cstdint>
#include <string>

struct sensor_data {
  double ax, ay, az;
  double gx, gy, gz;
  double mx, my, mz;
  double temperature;
  double battery;
  double airPressure;
};


struct sensor_property {
  std::uint8_t accFsr;
  std::uint16_t gyroFsr;
  float magXcoef, magYcoef, magZcoef;
};


extern void init_parser(sensor_property);
extern sensor_data *parse(void *data);

#endif /* end of include guard: PARSER_HEADER */
