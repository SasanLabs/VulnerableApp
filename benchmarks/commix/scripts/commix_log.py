#!/usr/bin/env python3
"""Parse a commix text log (``logs.txt``) into a structured, machine-readable report.

commix does not emit a JSON report in a stable release, but it always writes a
per-target ``logs.txt`` whose lines have a fixed ``[info] <field>: <value>``
shape. This module turns that text into the small dictionary the runner records
and the converter grades, so the fragile text parsing lives in one tested place.
"""
import re

# commix writes every log line through INFO_BOLD_SIGN, which renders as "[info] ".
_TESTED_URL = re.compile(r"^\[info\]\s*Tested URL\s*:\s*(\S+)\s*$")
_FIELD = re.compile(r"^\[info\]\s*(Type|Technique|Parameter)\s*:\s*(.+?)\s*$")
_EXECUTION = re.compile(
    r"^\[info\]\s*User-supplied command '(?P<command>.*)' execution output:\s*(?P<output>.*?)\s*$"
)
# The "(GET) ipaddress" shape commix uses for a non-header parameter.
_PARAMETER = re.compile(r"^\((?P<method>[A-Z]+)\)\s+(?P<parameter>.+)$")


def parse_log(text):
    """Return ``{"testedUrl", "injectionPoints", "executions"}`` for one log.

    ``injectionPoints`` holds one entry per confirmed injection point, each with
    its ``type``, ``technique``, ``method`` and ``parameter``. ``executions``
    holds every ``--os-cmd`` result, each with the ``command`` commix ran and the
    ``output`` the target returned. Both lists are empty when commix confirmed
    nothing, which is exactly what a secure level produces.
    """
    tested_url = None
    injection_points = []
    executions = []
    current = None
    for line in text.splitlines():
        line = line.rstrip()
        match = _TESTED_URL.match(line)
        if match:
            tested_url = match.group(1)
            continue
        match = _EXECUTION.match(line)
        if match:
            executions.append(
                {"command": match.group("command"), "output": match.group("output")}
            )
            continue
        match = _FIELD.match(line)
        if not match:
            continue
        field, value = match.group(1), match.group(2)
        # commix writes the three fields in order (Type, Technique, Parameter);
        # a "Type" line always opens a fresh injection point.
        if field == "Type" or current is None:
            current = {"type": None, "technique": None, "method": None, "parameter": None}
            injection_points.append(current)
        if field == "Type":
            current["type"] = value
        elif field == "Technique":
            current["technique"] = value
        elif field == "Parameter":
            param = _PARAMETER.match(value)
            if param:
                current["method"] = param.group("method")
                current["parameter"] = param.group("parameter")
            else:
                current["parameter"] = value
    return {
        "testedUrl": tested_url,
        "injectionPoints": injection_points,
        "executions": executions,
    }
