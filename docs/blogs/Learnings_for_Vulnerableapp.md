---
layout: default
title: Building a Scalable & Maintainable VulnerableApp
has_children: false
parent: Blogs
---

## 🚀 Building a Scalable & Maintainable VulnerableApp Architecture

Over the last few years, while building solutions to simulate real security vulnerabilities, one thing became very clear: **traditional vulnerable apps** often become **monolithic**, hard to extend, and tightly coupled to a specific stack, limiting **scalability** and **maintainability**.

To solve this, we leaned into **annotation-driven design**, **metadata contracts**, and a **distributed, modular architecture** — and the results speak for themselves.

---

## 🧠 1. Annotation-Driven Vulnerability Definitions

At the core of VulnerableApp’s architecture is a set of **custom annotations** that drive the vulnerability model, UI, and scanner interaction.

Instead of hand-coding endpoints or UI metadata, developers annotate classes with:

* `@VulnerableAppRestController` → expose REST endpoints + UI metadata  
* `@VulnerableAppRequestMapping` → attach levels (secure/unsecure) + templates  
* `@AttackVector` → describe exploit vectors and payloads  

From this **minimal metadata**, the system builds UI and scanner definitions dynamically. Adding a new vulnerability is as simple as adding a class with the right annotations — no boilerplate code.

💡 **Architectural Win:** Core model becomes **declarative**, **extensible**, **self-documenting** — huge win for scalability and onboarding.

---

## 🛠️ 2. Separation of Concerns — Backend vs UI

VulnerableApp exposes a single dynamic endpoint (`/VulnerabilityDefinitions`) returning metadata driven by annotations.

The UI builds itself from the contract. Every vulnerability includes names, templates, descriptions, and attack vectors.

This **decoupled API↔UI contract** improves maintainability and simplifies upgrades or rewrites.

---

## 🌐 3. Multi-Stack Support with VulnerableApp-facade

Scaling beyond Java/Spring required a multi-stack solution. Vulnerabilities exist in **Python, PHP, Node, Go**, and more.

**VulnerableApp-facade** is a **gateway + orchestrator**:

* Routes requests to multiple VulnerableApps based on URI patterns  
* Each app provides a schema/contract — if adhered, the facade:  
 ✔ Routes traffic  
 ✔ Merges definitions  
 ✔ Displays content in a shared UI shell  
* A generic skeleton UI reads the contract and embeds stack-specific content  

This design is:  
✔ Stack-agnostic  
✔ Lightweight  
✔ Easy to deploy as a distributed service farm

💡 **Architectural Win:** Decouples vulnerability store + UI from stack, enabling horizontal scaling.

---

## 📈 Why This Matters

✨ **Declarative Metadata via Annotations** → Add vulnerabilities without touching UI/infrastructure  
✨ **Contract-Based Modular Architecture** → Different stacks plug in seamlessly  
✨ **Dynamic UI Driven from API Definitions** → No hardcoded pages  
✨ **Tooling-Friendly Endpoints (DAST/SAST)** → Full automation and evaluation workflows  
✨ **Lightweight Gateway (OpenResty + Lua)** → Efficient orchestration without bloat

---

## 💡 Final Thought

By emphasizing **metadata contracts**, modular services, and clear UI/backend separation, we solved scalability and cross-stack extensibility problems that traditional vulnerable apps struggle with.

If you’re building extensible systems that need to grow without pain, this pattern is highly effective.
