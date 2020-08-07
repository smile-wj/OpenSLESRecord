
//

#include "record_buffer.h"

RecordBuffer::RecordBuffer(int buffersize) {
    buffer = new short *[6];
    for (int i = 0; i < 6; i++) {
        buffer[i] = new short[buffersize];
    }
}

RecordBuffer::~RecordBuffer() {

}

short *RecordBuffer::getRecordBuffer() {
    index++;
    if (index > 1) {
        index = 0;
    }
    return buffer[index];
}

short *RecordBuffer::getNowBuffer() {
    return buffer[index];
}