import requests
import time
import os
import logging

API_BASE_URL = "http://YOURIP:5700"
API_TOKEN = "YOUR_Authorization"

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def main():
    try:
        record_data = get_data("/api/scripts?t=")
        logger.info("获取到的body code: %s", record_data.get("code"))

        for val in record_data.get("data", []):
            try:
                if val.get("children"):
                    for child_val in val["children"]:
                        child_data = get_data("/api/scripts/%s?path=%s&t=" % (child_val["title"], child_val["parent"]))
                        download_file(child_data.get("data", ""), child_val["parent"], child_val["title"])
                else:
                    child_data = get_data("/api/scripts/%s?path=&t=" % val["title"])
                    download_file(child_data.get("data", ""), "", val["title"])
            except Exception as e:
                logger.error("处理子项目时出错: %s", str(e))
        logger.info("执行完毕")
    except Exception as e:
        logger.error("An error occurred: %s", str(e))

def get_data(url_str):
    try:
        timestamp = str(int(time.time() * 1000))
        full_url = API_BASE_URL + url_str + timestamp
        logger.info(full_url)
        headers = {"Authorization": "Bearer " + API_TOKEN}
        resp = requests.get(full_url, headers=headers)
        resp.raise_for_status()  # Raise an error for non-2xx responses
        return resp.json()
    except requests.exceptions.RequestException as e:
        raise Exception("Request error: " + str(e))

def download_file(body, path, name):
    try:
        file_path = os.path.join(".", name)
        if path:
            if not os.path.exists(path):
                os.makedirs(path)
                os.chmod(path, 0o777)
            file_path = os.path.join(path, name)
        with open(file_path, "wb") as f:
            if isinstance(body, str):
                # If body is a string, encode it to bytes before writing
                body = body.encode("utf-8")
            f.write(body)
    except Exception as e:
        raise Exception("File download error: " + str(e))

if __name__ == "__main__":
    main()
