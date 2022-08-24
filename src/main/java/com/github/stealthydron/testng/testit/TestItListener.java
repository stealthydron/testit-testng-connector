package com.github.stealthydron.testng.testit;

import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.AutotestResults;
import com.github.stealthydron.testng.allure.model.AllureResultsContainer;
import com.github.stealthydron.testng.allure.model.AllureResultsMapper;
import com.github.stealthydron.testng.allure.model.Link;
import com.google.gson.Gson;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.TestListenerAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestItListener extends TestListenerAdapter {

    private static final Logger logger = LogManager.getLogger(TestItListener.class);
    private final TestItSettings testItSettings = ConfigFactory.create(TestItSettings.class);
    private final TestItApi testItApi = new TestItApi(testItSettings.endpoint(), testItSettings.token());

    @Override
    public void onFinish(ITestContext context) {
        final String allureResultsDirectory = "target/allure-results";
        String configurationId = testItApi.getTestRunsClient().getTestRun(testItSettings.testRunId()).getTestResults().get(0).getConfigurationId();

        File[] files = new File(allureResultsDirectory).listFiles();
        if (files == null) {
            logger.error("Не удалось получить файлы из директории {}", allureResultsDirectory);
        } else {
            List<String> testResults = getAllureResults(files);
            List<AutotestResults> autotestResultsList = new ArrayList<>();
            for (String testResult : testResults) {
                AllureResultsContainer result = getResultsFromFile(testResult);
                if (result == null) {
                    logger.error("Не удалось получить результаты для {}", testResult);
                } else {
                    final String testCaseId = getTestId(result);

                    if (testCaseId.isEmpty()) {
                        logger.error("Не указана аннотация @AutotestId для {}", result.getFullName());
                    } else {
                        AutotestResults autotestResults = new AllureResultsMapper(testItApi, "target/allure-results/%s")
                                .mapToTestItResults(result);
                        autotestResults.setConfigurationId(configurationId);
                        String externalId = testItApi.getAutotestsClient().getAutoTest(testCaseId).getExternalId();
                        autotestResults.setAutoTestExternalId(externalId);
                        autotestResultsList.add(autotestResults);
                    }
                }
            }
            try {
                logger.info("Загрузка результатов тест-рана {}", autotestResultsList);
                System.out.println("autotestResultsList: " + autotestResultsList);
                testItApi.getTestRunsClient().setAutoTestsResults(testItSettings.testRunId(), autotestResultsList);
            } catch (Exception e) {
                logger.error("Не удалось загрузить результаты тест-рана {}", e.getMessage());
            }
        }
    }

    private String getTestId(AllureResultsContainer resultsContainer) {
        Link autotest = resultsContainer.getLinks()
                .stream()
                .filter(link -> link.getType().equals("autotest"))
                .findFirst()
                .orElse(null);
        if (autotest != null) {
            return autotest.getName();
        } else return "";
    }

    private AllureResultsContainer getResultsFromFile(final String fileName) {
        final String filePath = String.format("target/allure-results/%s", fileName);
        try {
            return new Gson().fromJson(new FileReader(filePath), AllureResultsContainer.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> getAllureResults(File[] files) {
        return Stream.of(files)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(name -> name.contains("result"))
                .collect(Collectors.toList());
    }
}
