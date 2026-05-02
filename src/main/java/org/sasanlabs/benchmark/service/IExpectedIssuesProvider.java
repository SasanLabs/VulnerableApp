package org.sasanlabs.benchmark.service;

import java.io.IOException;
import java.util.List;
import org.sasanlabs.benchmark.model.ExpectedIssue;

/**
 * Source of SAST ground truth — the set of known vulnerability sites in VulnerableApp's source
 * tree. Implementations can read from a CSV file, an annotation harvest, or any other store.
 */
public interface IExpectedIssuesProvider {

    List<ExpectedIssue> getExpectedIssues() throws IOException;
}
