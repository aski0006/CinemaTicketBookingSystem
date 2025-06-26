from selenium import webdriver
from selenium.webdriver.edge.service import Service as EdgeService
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import random
import re

DRIVER_PATH = r"D:\A盘：软件\WebEdegDriver\msedgedriver.exe"
START_URL = "https://movie.douban.com/explore"
SQL_PATH = (
    r"D:\Code\JavaCode\CinemaTicketBookingSystem\src\main\resources\movies_info.sql"
)


def generate_web_driver():
    edge_service = EdgeService(DRIVER_PATH)
    driver = webdriver.Edge(service=edge_service)
    return driver


def scroll_and_load(driver, min_count=120):
    driver.get(START_URL)
    wait = WebDriverWait(driver, 10)
    movies = set()
    while True:
        lis = driver.find_elements(
            By.XPATH, "/html/body/div[3]/div[1]/div/div[1]/div/div/div/div[2]/ul/li"
        )
        if len(lis) >= min_count:
            break
        try:
            load_more = wait.until(
                EC.element_to_be_clickable(
                    (
                        By.XPATH,
                        "/html/body/div[3]/div[1]/div/div[1]/div/div/div/div[2]/div/button",
                    )
                )
            )
            driver.execute_script("arguments[0].scrollIntoView();", load_more)
            load_more.click()
            time.sleep(2)
        except Exception as e:
            print("加载更多按钮不可用或已无更多内容", e)
            break
    return driver.find_elements(
        By.XPATH, "/html/body/div[3]/div[1]/div/div[1]/div/div/div/div[2]/ul/li"
    )


def parse_movie_li(li):
    # 解析 li 元素，返回字典
    try:
        title = li.find_element(By.CSS_SELECTOR, ".drc-subject-info-title-text").text
        poster_url = li.find_element(By.CSS_SELECTOR, ".drc-cover-pic").get_attribute(
            "src"
        )
        rating = li.find_element(By.CSS_SELECTOR, ".drc-rating-num").text
        info = li.find_element(By.CSS_SELECTOR, ".drc-subject-info-subtitle").text
        # info 例：2025 / 美国 加拿大 / 恐怖 / 导演 / 主演
        info_parts = info.split(" / ")
        release_date = info_parts[0] if len(info_parts) > 0 else ""
        genre = info_parts[2] if len(info_parts) > 2 else ""
        actors = info_parts[4] if len(info_parts) > 4 else ""
        director = info_parts[3] if len(info_parts) > 3 else ""
        detail_url = li.find_element(By.TAG_NAME, "a").get_attribute("href")
        return {
            "title": title,
            "poster_url": poster_url,
            "rating": rating,
            "release_date": release_date,
            "genre": genre,
            "actors": actors,
            "director": director,
            "detail_url": detail_url,
        }
    except Exception as e:
        print("解析li失败", e)
        return None


def parse_detail_page(driver, url):
    driver.execute_script("window.open(arguments[0]);", url)
    driver.switch_to.window(driver.window_handles[-1])
    time.sleep(2)
    try:
        description = driver.find_element(By.CSS_SELECTOR, ".related-info .all").text
    except:
        try:
            description = driver.find_element(
                By.CSS_SELECTOR, ".related-info span[property='v:summary']"
            ).text
        except:
            description = ""
    try:
        # 获取时长并提取数字
        duration_text = driver.find_element(
            By.CSS_SELECTOR, "span[property='v:runtime']"
        ).text
        # 提取数字部分
        duration = "".join(filter(str.isdigit, duration_text))
        # 如果没有提取到数字，设置默认值
        duration = duration if duration else "90"
    except:
        duration = "90"  # 设置默认时长为90分钟
    try:
        trailer_url = driver.find_element(
            By.CSS_SELECTOR, ".related-pic-video"
        ).get_attribute("href")
    except:
        trailer_url = ""
    driver.close()
    driver.switch_to.window(driver.window_handles[0])
    return description, duration, trailer_url


def main():
    driver = generate_web_driver()
    lis = scroll_and_load(driver, 120)
    movies = []
    for li in lis[:120]:
        info = parse_movie_li(li)
        if info:
            # Handle release_date format
            date_str = info.get("release_date", "").strip()

            # Try to find a YYYY-MM-DD pattern first
            match = re.search(r"(\d{4}-\d{2}-\d{2})", date_str)
            if match:
                info["release_date"] = match.group(1)
            # If it's just a 4-digit year, randomize month and day
            elif re.fullmatch(r"\d{4}", date_str):
                year = int(date_str)
                month = random.randint(1, 12)
                day = random.randint(1, 28)  # Up to 28 to be safe for all months
                info["release_date"] = f"{year:04d}-{month:02d}-{day:02d}"
            # If format is still incorrect or missing, use a default
            else:
                info["release_date"] = "2025-01-01"

            desc, duration, trailer = parse_detail_page(driver, info["detail_url"])
            info["description"] = desc
            info["duration"] = duration
            info["trailer_url"] = trailer
            info["status"] = "SHOWING"
            movies.append(info)
    driver.quit()
    # 生成SQL时不需要添加"分钟"
    with open(SQL_PATH, "w", encoding="utf-8") as f:
        for m in movies:
            sql = f"""INSERT INTO movie (title, director, actors, duration, release_date, description, poster_url, trailer_url, rating, genre, status) VALUES (
                '{m['title'].replace("'", "''")}', 
                '{m['director'].replace("'", "''")}', 
                '{m['actors'].replace("'", "''")}', 
                {m['duration']},  
                '{m['release_date']}', 
                '{m['description'].replace("'", "''")}', 
                '{m['poster_url']}', 
                '{m['trailer_url']}', 
                {m['rating']}, 
                '{m['genre']}', 
                '{m['status']}'
            );\n"""
            f.write(sql)
    print("已生成 movies_info.sql")


if __name__ == "__main__":
    main()
