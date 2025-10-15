package com.ai.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fine_policy")
public class FinePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fine amount per day
    private double amountPerDay = 1.0;

    public FinePolicy() {}

    public FinePolicy(Long id, double amountPerDay) {
        this.id = id;
        this.amountPerDay = amountPerDay;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getAmountPerDay() { return amountPerDay; }
    public void setAmountPerDay(double amountPerDay) { this.amountPerDay = amountPerDay; }
}
