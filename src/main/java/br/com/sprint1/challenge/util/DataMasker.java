package br.com.sprint1.challenge.util;

public final class DataMasker {

    private DataMasker() {
    }

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return cpf;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "***.***.***-**";
        }
        return cpf.replaceAll("\\d", "*");
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            return "(**) ****-****";
        }
        return phone.replaceAll("\\d", "*");
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(word.charAt(0));
            if (word.length() > 1) {
                sb.append("*".repeat(word.length() - 1));
            }
        }
        return sb.toString();
    }
}
