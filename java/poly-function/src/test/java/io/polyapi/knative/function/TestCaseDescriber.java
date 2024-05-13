package io.polyapi.knative.function;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.StringUtils.rightPad;

@Slf4j
public class TestCaseDescriber {
    private static final Integer LINE_LENGTH = 80;
    public static void describeCase(Integer caseNumber, String description) {
        describe(format("Case %s", caseNumber), description);
    }

    public static void describeErrorCase(Integer caseNumber, String description) {
        describe(format("Error case %s", caseNumber), description);
    }

    private static void describe(String caseName, String description) {
        String dashes = range(0, ((LINE_LENGTH - caseName.length() - 2) / 2))
                .boxed()
                .map(index -> "-")
                .collect(joining());
        log.info("\n{} {} {}{}\n| {} |\n{}", dashes, caseName, caseName.length()%2 == 0? "" : " ", dashes, Splitter.fixedLength(LINE_LENGTH - 4).splitToList(description).stream().map(line -> rightPad(line, LINE_LENGTH - 4)).collect(joining(" |\n| ")), range(0, LINE_LENGTH).boxed().map(index -> "-").collect(joining()));
    }
}
