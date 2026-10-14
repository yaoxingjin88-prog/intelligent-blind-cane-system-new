package com.ruoyi.service;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.mapper.AlarmRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRecordServiceTest {

    @Mock
    private AlarmRecordMapper alarmRecordMapper;

    @InjectMocks
    private AlarmRecordService alarmRecordService;

    @Test
    void capsRecentAlarmLimitAt1000() {
        when(alarmRecordMapper.getRecentAlarmRecords(anyInt())).thenReturn(Collections.<AlarmRecord>emptyList());

        alarmRecordService.getRecentAlarmRecords(5000);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(alarmRecordMapper).getRecentAlarmRecords(captor.capture());
        assertEquals(1000, captor.getValue());
    }

    @Test
    void defaultsNullLimitTo200() {
        when(alarmRecordMapper.getUnhandledAlarmRecords(anyInt())).thenReturn(Collections.emptyList());

        alarmRecordService.getUnhandledAlarmRecords(null);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(alarmRecordMapper).getUnhandledAlarmRecords(captor.capture());
        assertEquals(200, captor.getValue());
    }
}
