# Autodroid Trader - Tradescripts Engine

The Tradescripts Engine is a data-driven, page-aware automation framework designed to execute trading plans on Android trading applications. It provides an API to manage and execute tradescripts that automate trading operations on various APKs.

## Setup and Installation

### 1. Prerequisites

- Python 3.10 or higher
- Conda package manager

### 2. Clone the Repository

```bash
git clone <repository-url>
cd autodroid
```

### 3. Use Conda to Create and Activate Virtual Environment:

```bash
# Create virtual environment
cd d:/git/autodroid/autodroid-trader-executor/tradescripts
conda create -n autodroid python=3.13.5

# Activate virtual environment
conda activate autodroid
```

### 4. Install Dependencies

```bash
# Install dependencies from pyproject.toml
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts' && conda activate autodroid && pip install -e . -v --index-url https://pypi.tuna.tsinghua.edu.cn/simple

conda activate autodroid && pip install -e .
```

## Usage

### Starting the Server

To start the tradescripts API server:

```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts'; conda activate autodroid; python run_server.py
```

Or using uvicorn directly:

```bash
uvicorn main:app --host 0.0.0.0 --port 8008
```

The API will be available at `http://0.0.0.0:8008`

### API Endpoints

- `GET /api/tradescripts` - Get list of available tradescripts by scanning the apks directory
- `GET /` - Health check endpoint

## Project Structure

- `main.py` - FastAPI application with tradescript scanning functionality
- `run_server.py` - Script to run the server
- `pyproject.toml` - Project dependencies and configuration
- `tests/` - Test files for the API
- `app/src/main/assets/apks/` - Directory containing APK-specific tradescripts and configurations

## Testing

To run the tests:

```bash
python -m pytest tests/
```


## tools

### adb_dumper.py 使用方法

```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts';conda activate autodroid; python tools/adb_dumper.py
```

### adb_operator.py 使用方法

```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts';conda activate autodroid; python tools/adb_operator.py
```
#### 命令行参数

- `--device, -d`: 设备ID (默认: TDCDU17905004388)
- `--package, -p`: 应用包名 (默认: com.tdx.androidCCZQ)
- `--flow, -f`: 流程名称 (默认: general，可选: general, wang-ge-jiao-yi)
- `--method, -m`: 页面识别方法 (默认: 0)

#### 示例

使用默认流程 (general):
```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts';conda activate autodroid; python tools/adb_operator.py
```

指定 wang-ge-jiao-yi 流程:
```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts';conda activate autodroid; python tools/adb_operator.py --flow wang-ge-jiao-yi
```

指定设备和流程:
```bash
cd 'd:/git/autodroid/autodroid-trader-executor/tradescripts';conda activate autodroid; python tools/adb_operator.py -d YOUR_DEVICE_ID -f wang-ge-jiao-yi
```
