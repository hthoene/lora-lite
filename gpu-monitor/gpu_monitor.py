#!/usr/bin/env python3
import json
import shutil
import subprocess
import sys


def is_cmd_available(cmd: str) -> bool:
    return shutil.which(cmd) is not None


def query_nvidia():
    # Nutzung + VRAM über nvidia-smi im CSV-Format holen. [web:47][web:49][web:52]
    cmd = [
        "nvidia-smi",
        "--query-gpu=index,utilization.gpu,memory.used,memory.total",
        "--format=csv,noheader,nounits",
    ]
    out = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode("utf-8")
    lines = [l.strip() for l in out.splitlines() if l.strip()]
    gpus = []
    for line in lines:
        parts = [p.strip() for p in line.split(",")]
        if len(parts) != 4:
            continue
        idx, util, mem_used, mem_total = parts
        gpus.append(
            {
                "index": int(idx),
                "util": float(util),
                "mem_used": int(mem_used),
                "mem_total": int(mem_total),
            }
        )
    return {"backend": "nvidia", "gpus": gpus}


def query_amd():
    cmd = ["rocm-smi", "--showuse", "--showmeminfo", "vram", "--json"]
    out = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode("utf-8")

    # Alles vor dem ersten '{' wegschneiden (Warnungen entfernen)
    json_start = out.find("{")
    if json_start == -1:
        raise RuntimeError(f"Unexpected rocm-smi output, no '{{' found: {out!r}")
    json_str = out[json_start:]

    data = json.loads(json_str)

    gpus = []
    for gpu_id, gpu_info in data.items():
        try:
            util_str = gpu_info.get("GPU use (%)", "0")
            util = float(util_str)

            mem_total_str = gpu_info.get("VRAM Total Memory (B)", "0")
            mem_used_str = gpu_info.get("VRAM Total Used Memory (B)", "0")
            mem_total = int(mem_total_str) // (1024 * 1024)
            mem_used = int(mem_used_str) // (1024 * 1024)

            # gpu_id ist "card0", "card1" -> Nummer extrahieren
            index = int("".join(ch for ch in gpu_id if ch.isdigit()) or "0")

            gpus.append(
                {
                    "index": index,
                    "util": util,
                    "mem_used": mem_used,
                    "mem_total": mem_total,
                }
            )
        except Exception:
            continue

    return {"backend": "amd", "gpus": gpus}

def main():
    try:
        if is_cmd_available("nvidia-smi"):
            result = query_nvidia()
        elif is_cmd_available("rocm-smi"):
            result = query_amd()
        else:
            result = {"backend": "none", "gpus": []}
        print(json.dumps(result))
    except subprocess.CalledProcessError as e:
        print(json.dumps({"backend": "error", "error": e.output.decode("utf-8", "ignore")}), file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(json.dumps({"backend": "error", "error": str(e)}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
