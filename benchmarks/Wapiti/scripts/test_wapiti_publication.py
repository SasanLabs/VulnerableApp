"""Run the real workflow publication step against a local Git remote."""
import os
import shutil
import subprocess
from pathlib import Path

import yaml

WORKFLOW = Path(__file__).resolve().parents[3] / ".github/workflows/wapiti-benchmark.yml"
FINDINGS = "benchmarks/Wapiti/findings/wapiti-findings.json"
RESULTS = "benchmarks/Wapiti/wapiti-results.json"


def test_publication_pushes_matching_findings_and_results_with_a_clean_worktree(tmp_path):
    git = shutil.which("git")
    remote = tmp_path / "remote.git"
    checkout = tmp_path / "checkout"
    subprocess.run([git, "init", "--bare", str(remote)], check=True, capture_output=True)
    subprocess.run([git, "init", "-b", "master", str(checkout)], check=True, capture_output=True)

    def run_git(*args):
        return subprocess.run([git, *args], cwd=checkout, check=True,
                              capture_output=True, text=True)

    run_git("config", "commit.gpgsign", "false")
    run_git("config", "user.name", "Local test")
    run_git("config", "user.email", "test@example.invalid")
    for name in (FINDINGS, RESULTS):
        path = checkout / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('{"sample": true}\n')
    run_git("add", ".")
    run_git("commit", "-m", "samples")
    run_git("remote", "add", "origin", str(remote))
    run_git("push", "-u", "origin", "master")
    for name in (FINDINGS, RESULTS):
        (checkout / name).write_text('{"fresh": true}\n')

    # Redirect only the external push URL. Commit, rebase, and push use real Git.
    bin_dir = tmp_path / "bin"
    bin_dir.mkdir()
    shim = bin_dir / "git"
    shim.write_text(
        '#!/usr/bin/env python3\nimport os, sys\n'
        'args = sys.argv[1:]\n'
        'if args and args[0] == "push":\n'
        '    args[1] = os.environ["LOCAL_REMOTE"]\n'
        'os.execv(os.environ["REAL_GIT"], [os.environ["REAL_GIT"], *args])\n'
    )
    shim.chmod(0o755)
    sleep = bin_dir / "sleep"
    sleep.write_text("#!/bin/sh\nexit 0\n")
    sleep.chmod(0o755)
    workflow = yaml.safe_load(WORKFLOW.read_text())
    step = next(s for s in workflow["jobs"]["wapiti-benchmark"]["steps"]
                if s["name"] == "Commit Benchmark Results")
    env = {**os.environ, "PATH": str(bin_dir) + os.pathsep + os.environ["PATH"],
           "TOKEN": "local-test", "REF_NAME": "master", "REAL_GIT": git,
           "LOCAL_REMOTE": str(remote)}
    result = subprocess.run(["bash", "-e", "-c", step["run"]], cwd=checkout,
                            env=env, capture_output=True, text=True)
    assert result.returncode == 0, result.stdout + result.stderr
    assert run_git("status", "--porcelain").stdout == ""
    for name in (FINDINGS, RESULTS):
        published = subprocess.run([git, "--git-dir", str(remote), "show", f"master:{name}"],
                                   check=True, capture_output=True, text=True)
        assert published.stdout == '{"fresh": true}\n'
