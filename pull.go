import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

const (
	API_BASE_URL = "http://YOURIP:5700"
	API_TOKEN    = "YOUR_Authorization"
)

type Script struct {
	Title    string `json:"title"`
	Parent   string `json:"parent"`
	Children []struct {
		Title  string `json:"title"`
		Parent string `json:"parent"`
	} `json:"children"`
}

func main() {
	recordData, err := getData("/api/scripts?t=")
	if err != nil {
		log.Fatalf("An error occurred: %s", err)
	}

	log.Printf("获取到的body code: %s", recordData["code"])

	for _, val := range recordData["data"].([]interface{}) {
		script := val.(map[string]interface{})
		if script["children"] != nil {
			for _, childVal := range script["children"].([]interface{}) {
				childScript := childVal.(map[string]interface{})
				childData, err := getData(fmt.Sprintf("/api/scripts/%s?path=%s&t=", childScript["title"], childScript["parent"]))
				if err != nil {
					log.Printf("处理子项目时出错: %s", err)
					continue
				}
				downloadFile(childData["data"].(string), childScript["parent"].(string), childScript["title"].(string))
			}
		} else {
			childData, err := getData(fmt.Sprintf("/api/scripts/%s?path=&t=", script["title"]))
			if err != nil {
				log.Printf("处理子项目时出错: %s", err)
				continue
			}
			downloadFile(childData["data"].(string), "", script["title"].(string))
		}
	}

	log.Println("执行完毕")
}

func getData(urlStr string) (map[string]interface{}, error) {
	timestamp := strconv.FormatInt(time.Now().UnixNano()/int64(time.Millisecond), 10)
	fullURL := API_BASE_URL + urlStr + timestamp
	log.Println(fullURL)
	req, err := http.NewRequest("GET", fullURL, nil)
	if err != nil {
		return nil, fmt.Errorf("Request error: %s", err)
	}
	req.Header.Set("Authorization", "Bearer "+API_TOKEN)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("Request error: %s", err)
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("Request error: %s", err)
	}

	var data map[string]interface{}
	err = json.Unmarshal(body, &data)
	if err != nil {
		return nil, fmt.Errorf("Request error: %s", err)
	}

	return data, nil
}

func downloadFile(body string, path string, name string) error {
	filePath := filepath.Join(".", name)
	if path != "" {
		if _, err := os.Stat(path); os.IsNotExist(err) {
			err := os.MkdirAll(path, 0777)
			if err != nil {
				return fmt.Errorf("File download error: %s", err)
			}
		}
		filePath = filepath.Join(path, name)
	}

	err := ioutil.WriteFile(filePath, []byte(body), 0666)
	if err != nil {
		return fmt.Errorf("File download error: %s", err)
	}

	return nil
}