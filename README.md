# LoRA-Lite

LoRA-Lite is a Vaadin + Spring Boot application for managing LoRA training workflows using the `ai-toolkit` backend.  
It provides a simple, file-based workflow with clearly separated datasets, templates, configs, logs, and outputs.

## Why another GUI?

This project started as a personal tool, but the workflow turned out to be simple and pleasant enough to share.  
The code is not perfect, but it is practical, focused, and easy to understand for small to medium-sized experiments.  
The main goal is to make LoRA training on AMD GPUs as painless as possible, especially for users who just want a UI and a working setup rather than wiring everything manually.

This tool is not intended as a production-ready, multi-user system. It is designed for personal use, quick testing, and iterative experimentation.

## Features

- Web UI built with Vaadin Flow
- Configuration form for LoRA training jobs based on YAML templates
- Dataset management with:
    - Image upload
    - Per-image caption files
    - Inline image cropping
- Process view with:
    - GPU utilization and memory monitor
    - Sample image gallery from the latest run
- Log panel that tails the current training log
- Archiving of the current workflow (configs, dataset, output, logs) into timestamped folders

## Screenshots

![Dataset View](assets/images/dataset.png)  
![Configuration View](assets/images/settings.png)

## Requirements

- Java 17+ runtime (inside the container, provided by the image)
- Python 3 with ROCm + PyTorch stack for `ai-toolkit` and GPU monitoring
- Docker and docker-compose (recommended for running the full stack)
- A compatible AMD ROCm GPU setup on the host

## Configuration

LoRA-Lite uses externalized configuration for all relevant folders.  
By default, the application expects a `/workspace` layout inside the container, with subfolders for configs, dataset, output, logs, archive, templates, and monitor scripts.  
In a typical setup these are mapped to host directories (for persistence and easier access) via bind mounts.

You can adjust these paths through Spring configuration (for example, `application.yml`) and by changing the volume mappings in your compose file.

## Docker Setup

The project includes a Dockerfile and a docker-compose service definition optimized for AMD ROCm.

### Run with docker-compose

Example `docker-compose.yml` service:

    services:
        lora-lite:
            image: thoenehannes/lora-lite:latest
            container_name: lora-lite
            restart: unless-stopped
            user: "1000:1000"
            group_add:
                - "44"
                - "992"
            
            devices:
                - /dev/kfd
                - /dev/dri
            
            security_opt:
                - seccomp=unconfined
            
            ipc: host
            shm_size: "16g"
            
            ports:
                - "127.0.0.1:8080:8080"
            
            volumes:
                - ./target/app.jar:/workspace/app.jar
                - ./data/dataset:/workspace/dataset
                - ./data/output:/workspace/output
                - ./data/configs:/workspace/configs
                - ./data/logs:/workspace/logs
                - ./data/hf-cache:/workspace/.cache/huggingface
                - ./data/archive:/workspace/archive
                - ./data/templates:/workspace/templates
            
            environment:
                - VAADIN_PRODUCTION_MODE=true
                - AI_TOOLKIT_AUTH=${AI_TOOLKIT_AUTH:-password}
                - HF_HOME=/workspace/.cache/huggingface
                - TRANSFORMERS_CACHE=/workspace/.cache/huggingface
            
            working_dir: /workspace

Start the service:

    docker compose up -d


Then open `http://localhost:8080` in your browser.

## ai-toolkit Integration

LoRA-Lite uses [`ai-toolkit`](https://github.com/ostris/ai-toolkit) as backend for training.  
The Docker image clones `ai-toolkit` into `/workspace/ai-toolkit` and installs its Python dependencies.

Training is triggered from the UI by:

1. Building a `JobConfiguration` from the selected template and form fields
2. Writing it as `train.yaml` into `configs/latest`
3. Starting a Python process in `/workspace/ai-toolkit` using that config
4. Redirecting stdout/stderr to `logs/latest.txt`

The UI reads and tails `logs/latest.txt` to show live logs while training is running.

## GPU Monitoring

GPU stats are polled periodically by a small Python script, typically located at:

    /workspace/monitor/gpu_monitor.py

A Java service executes this script, parses its JSON output into a `GpuStats` model, and exposes the latest values to the UI.  
The process view shows GPU utilization, memory usage, and sample images from `output/latest/samples`.

## License

This project is licensed under the Apache License 2.0 – see the `LICENSE` file for details.