package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;

    private final String randomString = UUID.randomUUID().toString();

    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;


    @BeforeEach
    public void setUp(){
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor(randomString, SensorType.DOOR);
    }

//    Test1
    @Test
    @DisplayName("If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    public void ifAlarmArmedAndSensorActivated_ChangeSystemInToPendingAlarmStatus(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

//   Test2
    @Test
    @DisplayName("If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.")
    public void ifAlarmArmedAndSensorActivatedAndSystemIsInPendingAlarm_AlarmStatusToAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    Test3
    @Test
    @DisplayName("If pending alarm and all sensors are inactive, return to no alarm state.")
    public void ifPendingAlarmAndAllSensorsInactive_ReturnNoAlarmStatus(){
//        Set<Sensor> sensorsList = createSensors(5, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//        sensor.setActive(true);
//        for (Sensor sensor : sensorsList){
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
//        }
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    Test4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("If alarm is active, change in sensor state should not affect the alarm state.")
    public void ifAlarmActiveChangeInSensorStateShouldNotAffectAlarmState(boolean status){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(securityRepository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

//    Test5
    @Test
    @DisplayName("If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    public void ifActiveSensorIsActivatedAgainAndSystemInPendingState_changeStateToAlarmState(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    Test6
    @Test
    @DisplayName("If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    public void ifInactiveSensorIsDeactivatedAgain_stateShouldNotChange(){
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

//    Test7
    @Test
    @DisplayName("If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    public void ifCatFoundInImageAndHomeIsArmed_ChangeStateToAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    Test8
    @Test
    @DisplayName("If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    public void ifCatIsNotFoundInImageAndSensorsInactive_ChangeStateToNoAlarm(){
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        sensor.setActive(false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    Test9
    @Test
    @DisplayName("If the system is disarmed, set the status to no alarm.")
    public void ifSystemIsDisarmed_SetTheStatusNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    Test10
    @Test
    @DisplayName("If the system is armed, reset all sensors to inactive.")
    public void ifSystemArmed_ResetAllSensorsToInactive(){

        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < 3; i++){
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        for(Sensor sensor : sensors){
            sensor.setActive(true);
        }
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()) );
    }

//    Test11
    @Test
    @DisplayName("If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    public void ifSystemArmedAndCameraFoundCat_AlarmStatusToAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


//    Tests for coverage

    @Test
    public void testAddAndRemoveStatusListener(){
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    public void testAddAndRemoveSensor(){
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

//    @Test
//    public void testCoverageFor_handleSensorDeactivated(){
//        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
//        sensor.setActive(false);
//        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//        securityService.changeSensorActivationStatus(sensor, false);
//        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
//    }


//    this function will test sensor when alarm status is alarm and sensor is deactivated
    @Test
    void testCoverageFor_handleSensorDeactivated(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(false);
        securityService.changeSenorActivation();
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
}
