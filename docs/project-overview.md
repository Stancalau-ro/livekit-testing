# LiveKit Testing Framework

## Vision

**Become the go-to reference for online a/v streaming integration.**

## Mission

**Livekit-testing automates feature, version and configuration testing in a consistent and extendable way.**

---

## Project Overview

This framework provides comprehensive testing infrastructure for LiveKit real-time communication applications. It enables teams to validate WebRTC functionality, container orchestration, and streaming integrations with confidence.

## Core Value Propositions

### For Development Teams
- Reliable integration testing across LiveKit versions
- Automated browser-based WebRTC validation
- Consistent test environments through Docker containerization

### For QA Engineers
- BDD scenarios for readable, maintainable tests
- VNC recording for debugging failures
- Comprehensive coverage of LiveKit features

### For DevOps
- CI/CD ready infrastructure
- Automated container lifecycle management
- Version matrix testing support

## Current Capabilities

| Capability | Description |
|------------|-------------|
| Docker Orchestration | LiveKit, Redis, MinIO, Egress container management |
| WebRTC Testing | Browser automation with Selenium for video/audio streaming |
| BDD Framework | Cucumber-based scenarios with Gherkin syntax |
| Recording Support | VNC session recording for test debugging |
| Token Management | Comprehensive access token generation and validation |
| S3 Integration | MinIO testing for egress recording storage |
| Version Testing | Configurable LiveKit and Egress version testing |

## Strategic Pillars

### Consistency
- Reproducible test environments
- Cross-platform compatibility (Windows/Linux)
- Standardized state management patterns

### Extensibility
- Modular architecture for new container types
- Pluggable step definitions for custom scenarios
- Version-aware configuration system

### Reliability
- Proper resource cleanup and isolation
- Comprehensive error handling
- Automatic failure documentation via recordings

## Target Use Cases

1. **Version Compatibility Testing** - Validate applications against multiple LiveKit versions
2. **Feature Validation** - Ensure WebRTC features work correctly end-to-end
3. **Configuration Testing** - Test different LiveKit configurations and setups
4. **Regression Testing** - Catch regressions in CI/CD pipelines
5. **Integration Testing** - Validate component interactions (Redis, S3, Egress)

## Technology Foundation

- **Java 21+**: Modern language features and performance
- **TestContainers**: Docker-based integration testing
- **Cucumber**: Behavior-driven development framework
- **Selenium WebDriver**: Browser automation for WebRTC
- **LiveKit SDK**: Official server-side integration

## Documentation Index

- [Getting Started](../README.md)
- [Feature Files](../src/test/resources/features/)
