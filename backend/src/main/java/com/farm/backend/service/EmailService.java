package com.farm.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================
    // 👤 EMPLOYEE ACCOUNT CREATED
    // =========================
    @Async
    public void sendEmployeeCreated(String to, String name) {

        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Compte créé - FARM IA");

        message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre compte a été créé avec succès.\n\n" +
                "Votre affectation (département et horaires) vous sera communiquée prochainement.\n\n" +
                "Cordialement.\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 📧 EMAIL 1 – WELCOME (ON APPROVAL)
    // =========================
    @Async
    public void sendWelcomeEmail(String to, String name) {
        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenue");

        message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre profil est validé avec succès.\n\n" +
                "Vous êtes maintenant enregistré dans notre système.\n" +
                "Veuillez attendre votre affectation.\n\n" +
                "Merci.\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 🔐 MANAGER ACCOUNT (ACTIVATION + FACE)
    // =========================
    public void sendManagerAccount(String to, String password, String token) {

        if (to == null || to.isEmpty()) return;

        String link = "http://localhost:4200/#/activate?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("🔐 Activation compte MANAGER - FARM IA");

        message.setText(
                "Bonjour,\n\n" +
                "Votre compte MANAGER a été créé.\n\n" +
                "📧 Email : " + to + "\n" +
                "🔑 Mot de passe : " + password + "\n\n" +

                "👉 Activez votre compte ici :\n" +
                link + "\n\n" +

                "⚠️ IMPORTANT :\n" +
                "- Vous devez activer votre compte\n" +
                "- Ensuite enregistrer votre visage (Face Recognition)\n\n" +

                "Après cela, vous aurez accès à votre dashboard.\n\n" +

                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 🔥 VIEWER ACTIVATION EMAIL
    // =========================
    public void sendViewerAccount(String to, String password, String token) {

        if (to == null || to.isEmpty()) return;

        String link = "http://localhost:4200/#/activate?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("🔐 Activation compte VIEWER - FARM IA");

        message.setText(
                "Bonjour,\n\n" +
                "Votre compte VIEWER a été créé.\n\n" +
                "📧 Email : " + to + "\n" +
                "🔑 Mot de passe : " + password + "\n\n" +

                "👉 Activez votre compte ici :\n" +
                link + "\n\n" +

                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 📌 ASSIGNMENT / UPDATE
    // =========================
    public void sendAssignment(String to, String depName, LocalTime start, LocalTime end) {

        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("📢 Mise à jour Department - FARM IA");

        message.setText(
                "Bonjour,\n\n" +
                "Vous êtes affecté au département :\n\n" +
                "🏢 Nom : " + depName + "\n" +
                "⏰ Horaire : " + start + " → " + end + "\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 📧 EMAIL 2 – TEAM VALIDATION TO ADMIN
    // =========================
    @Async
    public void sendTeamValidationToAdmin(String to, String managerName, String departmentName, java.util.List<String> employeeNames, String schedule) {
        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Équipe validée");

        StringBuilder employees = new StringBuilder();
        for (String name : employeeNames) {
            employees.append("- ").append(name).append("\n");
        }

        message.setText(
                "Bonjour Admin,\n\n" +
                "Le manager " + managerName + " a validé une équipe.\n\n" +
                "Département: " + departmentName + "\n" +
                "Employés:\n" + employees.toString() + "\n" +
                "Horaire: " + schedule + "\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 📧 EMAIL 3 – MANAGER CONFIRMATION
    // =========================
    @Async
    public void sendManagerConfirmation(String to, String departmentName, java.util.List<String> employeeNames) {
        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirmation");

        StringBuilder employees = new StringBuilder();
        for (String name : employeeNames) {
            employees.append("- ").append(name).append("\n");
        }

        message.setText(
                "Bonjour,\n\n" +
                "Vous avez validé l'équipe avec succès.\n\n" +
                "Département: " + departmentName + "\n" +
                "Employés:\n" + employees.toString() + "\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // 📧 EMAIL 4 – ASSIGNMENT TO EMPLOYEE
    // =========================
    @Async
    public void sendAssignmentNotification(String to,
                                           String name,
                                           String departmentName,
                                           String managerName,
                                           String workSchedule) {

        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Affectation");

        message.setText(
                "Bonjour " + name + ",\n\n" +
                "Vous êtes affecté au département " + departmentName + "\n\n" +
                "Manager: " + managerName + "\n" +
                "Horaire: " + workSchedule + "\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    @Async
    public void sendAssignmentUpdated(String to, String name, String departmentName) {
        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mise à jour de l'affectation");

        message.setText(
                "Bonjour " + name + ",\n\n" +
                "Votre affectation a été mise à jour.\n\n" +
                "Nouveau département : " + departmentName + "\n\n" +
                "Merci.\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }

    // =========================
    // ❌ DELETE
    // =========================
    public void sendDelete(String to, String depName) {

        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("❌ Suppression Department - FARM IA");

        message.setText(
                "Bonjour,\n\n" +
                "Le département suivant a été supprimé :\n\n" +
                "🏢 Nom : " + depName + "\n\n" +
                "FARM IA"
        );

        mailSender.send(message);
    }
}