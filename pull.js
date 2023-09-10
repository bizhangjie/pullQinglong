const axios = require('axios');
const fs = require('fs');
const path = require('path');

const API_BASE_URL = "http://YOURIP:5700";
const API_TOKEN = "YOUR_Authorization";

// 设置日志
const logger = {
  info: console.log,
  error: console.error
};

async function main() {
  try {
    const recordData = await getData("/api/scripts?t=");
    logger.info("获取到的body code: ", recordData.code);

    for (const val of recordData.data || []) {
      try {
        if (val.children) {
          for (const childVal of val.children) {
            const childData = await getData(`/api/scripts/${childVal.title}?path=${childVal.parent}&t=`);
            downloadFile(childData.data, childVal.parent, childVal.title);
          }
        } else {
          const childData = await getData(`/api/scripts/${val.title}?path=&t=`);
          downloadFile(childData.data, "", val.title);
        }
      } catch (e) {
        logger.error("处理子项目时出错: ", e);
      }
    }
    logger.info("执行完毕");
  } catch (e) {
    logger.error("An error occurred: ", e);
  }
}

async function getData(urlStr) {
  try {
    const timestamp = String(Date.now());
    const fullUrl = API_BASE_URL + urlStr + timestamp;
    logger.info(fullUrl);
    const headers = { "Authorization": "Bearer " + API_TOKEN };
    const resp = await axios.get(fullUrl, { headers });
    return resp.data;
  } catch (e) {
    throw new Error("Request error: " + e);
  }
}

function downloadFile(body, filePath, name) {
  try {
    let file_path = path.join(".", name);
    if (filePath) {
      if (!fs.existsSync(filePath)) {
        fs.mkdirSync(filePath, { recursive: true });
        fs.chmodSync(filePath, 0o777);
      }
      file_path = path.join(filePath, name);
    }
    if (typeof body === "string") {
      // If body is a string, encode it to bytes before writing
      body = Buffer.from(body, "utf-8");
    }
    fs.writeFileSync(file_path, body);
  } catch (e) {
    throw new Error("File download error: " + e);
  }
}

main();