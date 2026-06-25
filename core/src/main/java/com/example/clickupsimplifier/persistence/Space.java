package com.example.clickupsimplifier.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("space")
public record Space(@Id String id, String name) {
}
