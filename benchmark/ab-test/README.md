# Apache Benchmark (ab) Tests for Todo API

This directory contains scripts and data files for benchmarking the Todo API using Apache Benchmark (ab) tool.

## Prerequisites

- Apache Benchmark (ab) tool installed
- Todo application running locally

## Installation

If you don't have Apache Benchmark installed, you can install it:

### Windows
Apache Benchmark is included with Apache HTTP Server. You can download it from:
https://www.apachelounge.com/download/

### Linux
```bash
# Debian/Ubuntu
sudo apt-get install apache2-utils

# RHEL/CentOS/Fedora
sudo yum install httpd-tools
```

### macOS
```bash
brew install httpd
```

## Usage

1. Make sure the Todo application is running locally
2. Run the benchmark scripts from this directory

### Creating Multiple TODOs

```bash
./create-multiple-todos.sh
```

This will send multiple POST requests to create new TODOs using the generated test data.

### Getting TODOs

```bash
./get-todos.sh
```

This will send multiple GET requests to retrieve TODOs.

## Configuration

You can modify the benchmark parameters in the shell scripts:

- `-n`: Number of requests to perform
- `-c`: Number of multiple requests to make at a time
- `-T`: Content-type header to use for POST data

## Test Data

Test data files are generated and stored in the `benchmark/build/test-data` directory, which is automatically created when running the scripts. This directory is excluded from version control via .gitignore.

The test data is generated using the `generate-test-data.kts` script, which creates multiple JSON files with random todo items.
