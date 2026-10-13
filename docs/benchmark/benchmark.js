/*
 * Scanner benchmark page. Reads the standardized benchmark dataset
 * (analytics/data/benchmark/{runs,findings}.csv) and renders it with Vega-Lite.
 * It only depends on the dataset's columns, never on a particular scanner.
 *
 * The dataset location comes from the container's data-base attribute; a
 * ?data=<url> query parameter overrides it to preview a fork or branch.
 */
(function () {
    "use strict";

    // Categorical slots, assigned to benchmarks in dataset order and never cycled.
    var SERIES_COLORS = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300", "#4a3aa7", "#e34948"];
    var OVERFLOW_COLOR = "#8a8984";
    var MATRIX_RAMP = ["#cde2fb", "#104281"];
    var INK = "#52514e";

    var STATUS = {
        detected: { label: "Detected", icon: "\u2713" },
        missed: { label: "Missed", icon: "\u2715" },
        unmatched: { label: "Unmatched", icon: "?" },
    };
    var STATUS_ORDER = ["detected", "missed", "unmatched"];
    var NO_CHALLENGE = "(not mapped to a challenge)";

    var VEGA_CONFIG = {
        background: null,
        font: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
        view: { stroke: null },
        axis: {
            labelColor: INK,
            titleColor: INK,
            titleFontWeight: "normal",
            labelFontSize: 12,
            titleFontSize: 12,
            domainColor: "#c9c8c3",
            tickColor: "#c9c8c3",
            gridColor: "#ecebe7",
        },
        legend: { labelColor: INK, labelFontSize: 12, titleColor: INK },
    };
    var EMBED_OPTIONS = {
        renderer: "svg",
        config: VEGA_CONFIG,
        actions: { export: true, source: false, compiled: false, editor: false },
    };

    var root = document.getElementById("benchmark-app");
    if (!root) {
        return;
    }
    var part = function (role) {
        return root.querySelector('[data-role="' + role + '"]');
    };
    var base = new URLSearchParams(window.location.search).get("data") || root.dataset.base;
    if (base.charAt(base.length - 1) !== "/") {
        base += "/";
    }

    var NUMERIC = { total_expected: "number", detected: "number", missed: "number", unmatched: "number", coverage: "number" };
    var loader = vega.loader();
    Promise.all([loader.load(base + "runs.csv"), loader.load(base + "findings.csv")])
        .then(function (texts) {
            var runs = vega.read(texts[0], { type: "csv", parse: NUMERIC });
            var findings = vega.read(texts[1], { type: "csv", parse: {} });
            render(runs, findings);
            part("status").hidden = true;
        })
        .catch(function (error) {
            part("status").textContent = "Could not load the benchmark data from " + base + " (" + error.message + ").";
            console.error(error);
        });

    function render(runs, findings) {
        var benchmarks = describeBenchmarks(runs, findings);
        renderSummary(benchmarks);
        renderTrend(runs, benchmarks);
        renderMatrix(findings, benchmarks);
        setUpDetails(findings, benchmarks);
    }

    /** One entry per benchmark, in dataset order, with its latest run and color. */
    function describeBenchmarks(runs, findings) {
        var byId = {};
        var order = [];
        function add(row) {
            if (!byId[row.benchmark]) {
                byId[row.benchmark] = { id: row.benchmark, scanner: row.scanner, variant: row.variant, scanType: row.scan_type };
                order.push(row.benchmark);
            }
        }
        // findings.csv lists benchmarks in the configured order; runs.csv adds retired ones.
        findings.forEach(add);
        runs.forEach(add);
        runs.forEach(function (run) {
            var b = byId[run.benchmark];
            if (!b.latest || run.run_date >= b.latest.run_date) {
                b.latest = run;
            }
        });
        return order.map(function (id, i) {
            var b = byId[id];
            b.label = b.variant ? b.scanner + " (" + b.variant + ")" : b.scanner;
            b.color = i < SERIES_COLORS.length ? SERIES_COLORS[i] : OVERFLOW_COLOR;
            b.hasDetectedItems = findings.some(function (f) {
                return f.benchmark === id && f.status === "detected";
            });
            // Results written before the endpoint listed detected items only carry counts.
            b.itemsComplete = b.hasDetectedItems || !b.latest || b.latest.detected === 0;
            return b;
        });
    }

    function renderSummary(benchmarks) {
        var rows = benchmarks
            .filter(function (b) {
                return b.latest;
            })
            .sort(function (a, b) {
                return b.latest.coverage - a.latest.coverage;
            });
        var table = el("table", { class: "bm-table" });
        table.appendChild(headRow(["Scanner", "Type", "Detected", "Missed", "Coverage", "Unmatched", "Last run"]));
        var body = el("tbody");
        rows.forEach(function (b) {
            var r = b.latest;
            var bar = el("span", { class: "bm-bar", "aria-hidden": "true" }, [el("span", { style: "width:" + Math.min(100, r.coverage) + "%" })]);
            body.appendChild(
                el("tr", {}, [
                    el("td", {}, [swatch(b.color), document.createTextNode(b.label)]),
                    el("td", {}, r.scan_type),
                    el("td", { class: "bm-num" }, r.detected + " / " + r.total_expected),
                    el("td", { class: "bm-num" }, String(r.missed)),
                    el("td", { class: "bm-num bm-coverage" }, [bar, document.createTextNode(r.coverage.toFixed(2) + "%")]),
                    el("td", { class: "bm-num" }, String(r.unmatched)),
                    el("td", {}, r.run_date),
                ])
            );
        });
        table.appendChild(body);
        part("summary").appendChild(table);
    }

    function renderTrend(runs, benchmarks) {
        var labels = benchmarks.map(function (b) {
            return b.label;
        });
        var colors = benchmarks.map(function (b) {
            return b.color;
        });
        var labelOf = {};
        benchmarks.forEach(function (b) {
            labelOf[b.id] = b.label;
        });
        var values = runs.map(function (r) {
            return { run_date: r.run_date, benchmark: r.benchmark, label: labelOf[r.benchmark], coverage: r.coverage };
        });
        var tooltip = [{ field: "run_date", type: "temporal", title: "Date", format: "%b %d, %Y" }];
        var tooltipText = [];
        benchmarks.forEach(function (b, i) {
            var source = "datum[" + JSON.stringify(b.id) + "]";
            tooltipText.push({ calculate: "isValid(" + source + ") ? format(" + source + ", '.2f') + '%' : 'no run'", as: "t" + i });
            tooltip.push({ field: "t" + i, type: "nominal", title: b.label });
        });
        // On a narrow screen the direct labels would leave no room for the plot; the legend still names each line.
        var narrow = part("trend").clientWidth < 560;
        var legend = narrow ? { orient: "top", title: null, columns: 1 } : { orient: "top", title: null };
        var x = { field: "run_date", type: "temporal", title: null, axis: { format: "%b %d", grid: false, labelOverlap: true } };
        var color = { field: "label", type: "nominal", scale: { domain: labels, range: colors }, legend: legend };
        var endpoint = [{ mark: { type: "point", filled: true, size: 70, opacity: 1 } }];
        if (!narrow) {
            endpoint.push({
                mark: { type: "text", align: "left", dx: 8, fontSize: 12, color: INK },
                encoding: { text: { field: "label" }, color: { value: INK } },
            });
        }
        var spec = {
            $schema: "https://vega.github.io/schema/vega-lite/v5.json",
            width: "container",
            height: 280,
            padding: { right: narrow ? 8 : 150 },
            data: { values: values },
            encoding: { x: x },
            layer: [
                {
                    mark: { type: "line", strokeWidth: 2 },
                    encoding: {
                        y: { field: "coverage", type: "quantitative", title: "Coverage (%)" },
                        color: color,
                    },
                },
                {
                    // Endpoint marker and direct label, so a series with a single run is still visible.
                    transform: [
                        { joinaggregate: [{ op: "max", field: "run_date", as: "last_date" }], groupby: ["benchmark"] },
                        { filter: "datum.run_date === datum.last_date" },
                    ],
                    encoding: { y: { field: "coverage", type: "quantitative" }, color: color },
                    layer: endpoint,
                },
                {
                    // Crosshair: the nearest date, with every benchmark's coverage on that day.
                    transform: [{ pivot: "benchmark", value: "coverage", groupby: ["run_date"] }].concat(tooltipText),
                    mark: { type: "rule", color: "#a3a29d", strokeWidth: 1 },
                    encoding: {
                        opacity: { condition: { param: "hover", empty: false, value: 1 }, value: 0 },
                        tooltip: tooltip,
                    },
                    params: [
                        {
                            name: "hover",
                            select: { type: "point", fields: ["run_date"], nearest: true, on: "pointerover", clear: "pointerout" },
                        },
                    ],
                },
            ],
        };
        vegaEmbed(part("trend"), spec, EMBED_OPTIONS);
    }

    function renderMatrix(findings, benchmarks) {
        var complete = {};
        var labelOf = {};
        benchmarks.forEach(function (b) {
            complete[b.id] = b.itemsComplete;
            labelOf[b.id] = b.label;
        });
        var cells = {};
        findings.forEach(function (f) {
            if (f.status === "unmatched" || !f.challenge) {
                return;
            }
            var key = f.benchmark + "|" + f.challenge;
            var cell = cells[key] || (cells[key] = { benchmark: f.benchmark, label: labelOf[f.benchmark], challenge: f.challenge, detected: 0, missed: 0 });
            cell[f.status] += 1;
        });
        var values = Object.keys(cells).map(function (key) {
            var c = cells[key];
            c.expected = c.detected + c.missed;
            if (complete[c.benchmark]) {
                c.rate = c.detected / c.expected;
                c.text = c.detected + "/" + c.expected;
            } else {
                // Without detected items only the misses are known, not the expected total.
                c.rate = null;
                c.text = c.missed + " missed";
            }
            return c;
        });

        var pending = benchmarks.filter(function (b) {
            return !b.itemsComplete;
        });
        if (pending.length) {
            var note = part("matrix-note");
            note.textContent =
                "Per-challenge detections for " +
                pending.map(function (b) { return b.label; }).join(", ") +
                " appear after that scanner's next benchmark run. Until then their cells, in gray, only count what was missed.";
            note.hidden = false;
        }

        var challenges = unique(values.map(function (v) { return v.challenge; })).sort(caseInsensitive);
        var labels = benchmarks.map(function (b) { return b.label; });
        var spec = {
            $schema: "https://vega.github.io/schema/vega-lite/v5.json",
            data: { values: values },
            width: { step: 120 },
            height: { step: 24 },
            encoding: {
                x: { field: "label", type: "nominal", sort: labels, title: null, axis: { orient: "top", labelAngle: 0, labelLimit: 115, domain: false, ticks: false } },
                y: { field: "challenge", type: "nominal", sort: challenges, title: null, axis: { domain: false, ticks: false, labelLimit: 260 } },
                tooltip: [
                    { field: "challenge", title: "Challenge" },
                    { field: "label", title: "Scanner" },
                    { field: "detected", title: "Detected" },
                    { field: "missed", title: "Missed" },
                    { field: "rate", title: "Coverage", format: ".0%" },
                ],
            },
            layer: [
                {
                    params: [{ name: "pick", select: { type: "point", fields: ["challenge"], on: "click" } }],
                    mark: { type: "rect", cornerRadius: 3, stroke: "#ffffff", strokeWidth: 2, cursor: "pointer" },
                    encoding: {
                        color: {
                            condition: { test: "datum.rate === null", value: "#dcdbd6" },
                            field: "rate",
                            type: "quantitative",
                            scale: { domain: [0, 1], range: MATRIX_RAMP },
                            legend: { title: "Coverage", format: ".0%", orient: "top", gradientLength: 160 },
                        },
                    },
                },
                {
                    mark: { type: "text", fontSize: 11 },
                    encoding: {
                        text: { field: "text" },
                        color: { condition: { test: "datum.rate !== null && datum.rate > 0.5", value: "#ffffff" }, value: "#27262b" },
                    },
                },
            ],
        };
        vegaEmbed(part("matrix"), spec, EMBED_OPTIONS).then(function (result) {
            result.view.addEventListener("click", function (event, item) {
                if (item && item.datum && item.datum.challenge) {
                    showChallenge(item.datum.challenge);
                }
            });
        });
    }

    var details = {};

    function setUpDetails(findings, benchmarks) {
        details.findings = findings;
        details.benchmarks = {};
        benchmarks.forEach(function (b, i) {
            details.benchmarks[b.id] = { label: b.label, color: b.color, order: i };
        });
        var select = part("challenge-select");
        var challenges = unique(findings.map(function (f) { return f.challenge; })).filter(Boolean).sort(caseInsensitive);
        if (findings.some(function (f) { return !f.challenge; })) {
            challenges.push(NO_CHALLENGE);
        }
        challenges.forEach(function (c) {
            select.appendChild(el("option", { value: c }, c));
        });
        select.addEventListener("change", renderDetails);
        part("status-select").addEventListener("change", renderDetails);
        renderDetails();
    }

    function showChallenge(challenge) {
        part("challenge-select").value = challenge;
        renderDetails();
        part("challenge-select").scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function renderDetails() {
        var challenge = part("challenge-select").value;
        var status = part("status-select").value;
        var rows = details.findings
            .filter(function (f) {
                return (f.challenge || NO_CHALLENGE) === challenge && (!status || f.status === status);
            })
            .sort(function (a, b) {
                return (
                    details.benchmarks[a.benchmark].order - details.benchmarks[b.benchmark].order ||
                    STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status) ||
                    levelNumber(a.level) - levelNumber(b.level) ||
                    a.location.localeCompare(b.location)
                );
            });
        var container = part("details");
        container.textContent = "";
        if (!rows.length) {
            container.appendChild(el("p", { class: "bm-help" }, "No findings for this selection."));
            return;
        }
        var table = el("table", { class: "bm-table" });
        table.appendChild(headRow(["Scanner", "Status", "Level", "Vulnerability type", "CWE", "Location"]));
        var body = el("tbody");
        rows.forEach(function (f) {
            var b = details.benchmarks[f.benchmark];
            var s = STATUS[f.status] || { label: f.status, icon: "" };
            body.appendChild(
                el("tr", {}, [
                    el("td", {}, [swatch(b.color), document.createTextNode(b.label)]),
                    el("td", {}, [el("span", { class: "bm-pill bm-pill--" + f.status }, s.icon + " " + s.label)]),
                    el("td", {}, f.level || "\u2013"),
                    el("td", {}, f.vulnerability_type || "\u2013"),
                    el("td", {}, f.cwe || "\u2013"),
                    el("td", { class: "bm-location" }, f.location || "\u2013"),
                ])
            );
        });
        table.appendChild(body);
        container.appendChild(el("p", { class: "bm-help" }, rows.length + (rows.length === 1 ? " finding" : " findings")));
        container.appendChild(table);
    }

    function el(tag, attrs, children) {
        var node = document.createElement(tag);
        Object.keys(attrs || {}).forEach(function (name) {
            node.setAttribute(name, attrs[name]);
        });
        if (typeof children === "string") {
            node.textContent = children;
        } else if (children) {
            children.forEach(function (child) {
                node.appendChild(child);
            });
        }
        return node;
    }

    function headRow(titles) {
        return el("thead", {}, [
            el(
                "tr",
                {},
                titles.map(function (t) {
                    return el("th", { scope: "col" }, t);
                })
            ),
        ]);
    }

    function swatch(color) {
        return el("span", { class: "bm-swatch", style: "background:" + color, "aria-hidden": "true" });
    }

    function unique(items) {
        return items.filter(function (item, i) {
            return items.indexOf(item) === i;
        });
    }

    function caseInsensitive(a, b) {
        return a.localeCompare(b, undefined, { sensitivity: "base" });
    }

    function levelNumber(level) {
        var m = /^LEVEL_(\d+)$/.exec(level || "");
        return m ? Number(m[1]) : -1;
    }
})();
