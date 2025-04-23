package com.rxclinic.controller;

import com.rxclinic.model.Appointment;
import com.rxclinic.model.Card;
import com.rxclinic.model.User;
import com.rxclinic.repository.AppointmentRepository;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody AppointmentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_DOCTOR") || role.getName().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Only patients can create appointments");
        }

        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setCard(card);
        appointment.setStatus("На ожидании");
        appointment.setCreatedAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(savedAppointment);
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_DOCTOR") || role.getName().equals("ROLE_ADMIN"))) {
            List<Appointment> appointments = appointmentRepository.findAll();
            return ResponseEntity.ok(appointments);
        } else {
            List<Appointment> appointments = appointmentRepository.findByUserId(user.getId());
            return ResponseEntity.ok(appointments);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<Appointment>> getUserAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Appointment> appointments = appointmentRepository.findByUserId(user.getId());
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_DOCTOR") || role.getName().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        List<Appointment> appointments = appointmentRepository.findAll();
        return ResponseEntity.ok(appointments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getUser().getId().equals(user.getId()) &&
                !user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_DOCTOR"))) {
            throw new RuntimeException("Access denied");
        }

        appointmentRepository.delete(appointment);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User doctor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!doctor.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_DOCTOR"))) {
            throw new RuntimeException("Only doctors can confirm appointments");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setDoctor(doctor);
        appointment.setStatus("Подтверждено");
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User doctor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!doctor.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_DOCTOR"))) {
            throw new RuntimeException("Only doctors can cancel appointments");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setDoctor(doctor);
        appointment.setStatus("Отменено");
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return ResponseEntity.ok(updatedAppointment);
    }

    public static class AppointmentRequest {
        private Long cardId;

        public Long getCardId() {
            return cardId;
        }

        public void setCardId(Long cardId) {
            this.cardId = cardId;
        }
    }
}