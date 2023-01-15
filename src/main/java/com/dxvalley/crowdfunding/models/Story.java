package com.dxvalley.crowdfunding.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class Story {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long storyId;

    private String decription;
    private String risks;

}
