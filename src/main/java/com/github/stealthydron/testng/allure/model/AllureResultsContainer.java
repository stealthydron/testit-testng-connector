package com.github.stealthydron.testng.allure.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AllureResultsContainer {

    private String name;
    private String fullName;
    private String status;
    private StatusDetails statusDetails;
    private List<Link> links;
    private List<AllureResultsStep> steps;
    private List<Parameter> parameters;
    private Long start;
    private Long stop;
}
