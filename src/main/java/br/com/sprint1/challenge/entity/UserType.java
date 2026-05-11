package br.com.sprint1.challenge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_type")
public class UserType {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "type", nullable = false)
    private String type;

    public UserType() {}

    public UserType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}