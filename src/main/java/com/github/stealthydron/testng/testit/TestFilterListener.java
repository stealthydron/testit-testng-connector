package com.github.stealthydron.testng.testit;

import com.github.avpyanov.testit.annotations.AutotestId;
import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.TestResult;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.List;

public class TestFilterListener implements IMethodInterceptor {

    private static final Logger logger = LogManager.getLogger(TestFilterListener.class);

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> list, ITestContext iTestContext) {
        final List<IMethodInstance> result = new ArrayList<>();
        final List<String> testIdList = getAutotestIdsFromTestRun();
        for (IMethodInstance iMethodInstance : list) {
            String testId = getTestId(iMethodInstance);
            if (testIdList.contains(testId)) {
                result.add(iMethodInstance);
            }
        }
        return result;
    }

    private List<String> getAutotestIdsFromTestRun() {
        final List<String> testIds = new ArrayList<>();
        final TestItSettings testItSettings = ConfigFactory.create(TestItSettings.class);
        final TestItApi testItApi = new TestItApi(testItSettings.endpoint(), testItSettings.token());

        try {
            logger.info("Получение id тестов из тест-рана");
            List<TestResult> results = testItApi.getTestRunsClient().getTestRun(testItSettings.testRunId()).getTestResults();
            for (TestResult result : results) {
                testIds.add(result.getAutoTest().getGlobalId());
            }
            logger.info("id тестов для запуска {}", testIds);
            return testIds;
        } catch (Exception e) {
            logger.error("не удалось получить список тестов {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private String getTestId(IMethodInstance instance) {
        AutotestId autotestId = instance.getMethod()
                .getConstructorOrMethod()
                .getMethod()
                .getAnnotation(AutotestId.class);
        if (autotestId != null) {
            return autotestId.value();
        } else return "";
    }
}