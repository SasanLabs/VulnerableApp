---
layout: default
title: Scanner Benchmark
nav_order: 6
---

# Scanner Benchmark

How well do security scanners find the vulnerabilities VulnerableApp ships? Each
scanner below runs against VulnerableApp every night, and its findings are graded
against the application's ground truth: the list of vulnerable challenge levels
(DAST) and vulnerable source lines (SAST).
{: .fs-5 .fw-300 }

The page reads the standardized benchmark dataset, so a scanner added to the
pipeline shows up here without any change to this page. See
[`analytics/`](https://github.com/SasanLabs/VulnerableApp/tree/master/analytics)
for how the dataset is built and what each column means.

<link rel="stylesheet" href="benchmark.css">

<div id="benchmark-app"
     data-base="https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/analytics/data/benchmark/">

  <p class="bm-status" data-role="status" role="status">Loading benchmark data…</p>

  <section class="bm-section" aria-labelledby="bm-summary-title">
    <h2 id="bm-summary-title">Latest results</h2>
    <p class="bm-help">
      <strong>Coverage</strong> is the share of expected vulnerabilities the scanner
      detected. <strong>Unmatched</strong> counts reported items that don't line up
      with any expected vulnerability: hardening alerts, findings on secure levels,
      or code outside the ground truth. They are not necessarily false positives.
    </p>
    <div class="bm-table-wrap" data-role="summary"></div>
  </section>

  <section class="bm-section" aria-labelledby="bm-trend-title">
    <h2 id="bm-trend-title">Coverage over time</h2>
    <p class="bm-help">One point per day a scanner's results changed.</p>
    <div class="bm-chart" data-role="trend"></div>
  </section>

  <section class="bm-section" aria-labelledby="bm-matrix-title">
    <h2 id="bm-matrix-title">Detection by challenge</h2>
    <p class="bm-help">
      Share of each challenge's expected vulnerabilities that a scanner detected.
      Select a cell to see its findings below.
    </p>
    <p class="bm-note" data-role="matrix-note" hidden></p>
    <div class="bm-chart bm-chart--scroll" data-role="matrix"></div>
  </section>

  <section class="bm-section" aria-labelledby="bm-drill-title">
    <h2 id="bm-drill-title">Challenge details</h2>
    <div class="bm-filters">
      <label>Challenge
        <select data-role="challenge-select"></select>
      </label>
      <label>Status
        <select data-role="status-select">
          <option value="">All</option>
          <option value="detected">Detected</option>
          <option value="missed">Missed</option>
          <option value="unmatched">Unmatched</option>
        </select>
      </label>
    </div>
    <div class="bm-table-wrap" data-role="details"></div>
  </section>
</div>

<script src="https://cdn.jsdelivr.net/npm/vega@5.30.0/build/vega.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/vega-lite@5.21.0/build/vega-lite.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/vega-embed@6.26.0/build/vega-embed.min.js"></script>
<script src="benchmark.js"></script>
