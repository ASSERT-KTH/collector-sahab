package se.assertkth.tracediff.statediff.models;

public class SelectedTest {
    private String testName, testLink;

    public SelectedTest(String testName, String testLink){
        this.testName = testName;
        this.testLink = testLink;
    }

    public String getTestLink() {
        return testLink;
    }

    public void setTestLink(String testLink) {
        this.testLink = testLink;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
}
