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

/**
 * {@link AlarmRecordService} 单测：列表查询 limit 规范化（上限 / 默认值）
 */
@ExtendWith(MockitoExtension.class)
class AlarmRecordServiceTest {

    @Mock
    private AlarmRecordMapper alarmRecordMapper;

    @InjectMocks
    private AlarmRecordService alarmRecordService;

    /** 传入超大 limit 时，下传 Mapper 应被封顶为 1000 */
    @Test
    void capsRecentAlarmLimitAt1000() {
        when(alarmRecordMapper.getRecentAlarmRecords(anyInt())).thenReturn(Collections.<AlarmRecord>emptyList());

        alarmRecordService.getRecentAlarmRecords(5000);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(alarmRecordMapper).getRecentAlarmRecords(captor.capture());
        assertEquals(1000, captor.getValue());
    }

    /** 未处理列表 limit 为 null 时，默认 200 */
    @Test
    void defaultsNullLimitTo200() {
        when(alarmRecordMapper.getUnhandledAlarmRecords(anyInt())).thenReturn(Collections.emptyList());

        alarmRecordService.getUnhandledAlarmRecords(null);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(alarmRecordMapper).getUnhandledAlarmRecords(captor.capture());
        assertEquals(200, captor.getValue());
    }
}
