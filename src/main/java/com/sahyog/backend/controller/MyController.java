package com.sahyog.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahyog.backend.entities.*;
import com.sahyog.backend.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@RestController

public class MyController {
    String accessToken;
    private static CustomResponse asyncCustomResponse = new CustomResponse();
    public List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    public List<SseEmitter> emitters2 = new CopyOnWriteArrayList<>();
    @GetMapping("/home")
    public String home()  {
        System.out.println("home");
        return "";
    }
    //Receiving Callback APIs from ABDM
    @PostMapping("/v0.5/users/auth/on-init")
    public void onInit(@RequestBody String response) throws Exception {
        System.out.println("Response on init " + response);
        String keyPath = "auth\":{\"transactionId\"";
        String transactionId = new Util().getValueFromString(keyPath, response);
        asyncCustomResponse.setTransactionId(transactionId);
        for(SseEmitter emitter :emitters){
            emitter.send((asyncCustomResponse));
        }
    }
    @PostMapping("/v0.5/users/auth/on-confirm")
    public void onConfirm(@RequestBody String response) throws Exception {

        accessToken = Util.getAccessToken(response);
//        post
        System.out.println("AccessToken arrived  " + accessToken);

        System.out.println("Response on confirm " + response);
        String keyPath1 = "patient\":";
        String keyPath2 = "\"error\"";
        String patient = new Util().getValueFromString2(keyPath1,keyPath2, response);

        asyncCustomResponse.setPatient(patient);
        for(SseEmitter emitter :emitters2){
            emitter.send((asyncCustomResponse));
        }
    }

    //Custom APIS for FRONTEND
    @PostMapping(value = "/api/register/health-id")
    public SseEmitter registerPatientUsinghealthId(@RequestBody CustomRequest customRequest) throws Exception, IOException {
        System.out.println("Patient Abha Address : " + customRequest.getHealthId());
        ABDMSession session = new ABDMSession();
        session.setToken();
        System.out.println("retreived token : " + session.getToken());
        int statusCode = session.patientInitUsingMobile(customRequest.getHealthId()); //Calling abha to init patient using mobile otp
        SseEmitter sseEmitter = new SseEmitter((long)1000);
        if (statusCode == 202) {
//            sseEmitter.send(SseEmitter.event().name("INIT"));
            System.out.println("INIT Patient using mobile (sent to abha): " + statusCode);
        } else {
            System.out.println("Authentication Error : " + statusCode);
        }
        sseEmitter.onCompletion(()->emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }



    @PostMapping(value = "/api/register/confirmMobileOTP")
    public SseEmitter confirmMobileOTP(@RequestBody CustomRequest customRequest) throws Exception {
        System.out.println(customRequest.toString());
        System.out.println("Patient Abha Address : " + customRequest.getHealthId());
        ABDMSession session = new ABDMSession();
        session.setToken();
        System.out.println("retreived token : " + session.getToken());
        int statusCode = session.confirmMobileOTP(customRequest.getTransactionId(), customRequest.getMobileOTP());
        System.out.println("COmfirm Mobbile OTP Status Code: " + statusCode);

        SseEmitter sseEmitter = new SseEmitter((long)3000);
        sseEmitter.onCompletion(()->emitters2.remove(sseEmitter));
        emitters2.add(sseEmitter);
        return sseEmitter;


    }

    @GetMapping(value = "/api/link/care-context")
    public SseEmitter linkingCareContext() throws Exception, IOException {
        ABDMSession session = new ABDMSession();
        session.setToken();
        System.out.println("retreived token : " + session.getToken());
        int statusCode = session.careContextLinking(accessToken);
        SseEmitter sseEmitter = new SseEmitter((long)1000);
        if (statusCode == 202) {
            System.out.println("INIT Patient using mobile (sent to abha): " + statusCode);
        } else {
            System.out.println("Authentication Error : " + statusCode);
        }
        sseEmitter.onCompletion(()->emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }



//    ---------Patient Services------------
        @Autowired
        private PatientService patientService;

    @PostMapping("/api/patient/save")
    public boolean SavePatient(@RequestBody Patient patient)
    {
        System.out.println("Patient : "+patient.toString());
        return patientService.savePatient(patient);
    }

    @PostMapping("/api/patient/details")
    public List<Patient> getAllPatients()
    {
        return patientService.findDetails();
    }
    @PostMapping("/api/patient/{healthId}")
    public Patient getPatient(@PathVariable String healthId)
    {
        System.out.println("asdf "+ healthId);
        return patientService.findPatientByHealthId(healthId);
    }

//    ---------Doctor Services-------------------

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/api/doctor/care-context/create")
    public CareContext createNewCareContext(@RequestBody CareContext careContext)
    {
        careContext.patient = patientService.findPatientByHealthId(careContext.patient.healthId);

        return doctorService.addCareContext(careContext);
    }



//    ---------Admin Doctor services-------------
    @Autowired
    private AdminService adminDoctorService;

    @PostMapping("/api/admin/addDoctor")
    public Doctor saveDoctor(@RequestBody Doctor doctor)
    {
        return adminDoctorService.addDoctor(doctor);
    }
//    @CrossOrigin(origins = "http://172.16.134.145:3000")
    @PostMapping(value="/api/admin/getAllDoctors")
    public List<Doctor> getAllDoctors()
    {
        System.out.println("asdf");
        return adminDoctorService.findDoctors();
    }


    @PostMapping("/api/admin/getDoctor/{healthIdNumber}")
    public Doctor getDoctor(@PathVariable String healthIdNumber)
    {
        return adminDoctorService.findDoctorByHealthId(healthIdNumber);
    }

//    @GetMapping("/api/admin/getDoctor/{id}")
//    public Doctor getDoctor(@PathVariable int id)
//    {
//        return adminDoctorService.findDoctorById(id);
//    }

    @DeleteMapping("/api/admin/deleteDoctor/{id}")
    public String deleteDoctor(@PathVariable int id)
    {
        return adminDoctorService.deleteDoctor(id);
    }

    @PutMapping("/api/admin/updateDoctor")
    public Doctor updateDoctor(@RequestBody Doctor doctor)
    {
        return adminDoctorService.updateDoctor(doctor);
    }


}
