import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/*
    경/위도 (현재위치용)
    http://apis.data.go.kr/B552657/AEDInfoInqireService/getAedLcinfoInqire?WGS84_LON=126.324384&WGS84_LAT=33.464983&serviceKey=85pxiQNHO6gsxSjBFQfzN5lxPOIub30SlkWNkEvKSFjX%2BBl0sCbOltv6etE002jZB5OQkf9LFYqVZgpr2%2FivQg%3D%3D&
    제주도 애월 33.464983, 126.324384

    검색용
    http://apis.data.go.kr/B552657/AEDInfoInqireService/getEgytAedManageInfoInqire?Q0=%EB%B6%80%EC%82%B0%EA%B4%91%EC%97%AD%EC%8B%9C&Q1=%EC%98%81%EB%8F%84%EA%B5%AC&serviceKey=85pxiQNHO6gsxSjBFQfzN5lxPOIub30SlkWNkEvKSFjX%2BBl0sCbOltv6etE002jZB5OQkf9LFYqVZgpr2%2FivQg%3D%3D&
    Q0 = 시도 (ex. 서울, 서울특별시, 제주, 전라, 경상 (경남 충남 X 줄임말 안됨) )
    Q1 = 시군구 (ex. 제주, 영도, 사상)
*/

fun main() {
    var address = "" // 이 곳의 주소
    var place = "" // 설치된 위치
    var tel = "" // 관리자 연락처
    var model = "" // AED 모델명
    var org = "" // 설치된 기관명
    var addrLat = 0 // 위도
    var addrLon = 0 // 경도

    val lon = 126.324384
    val lat = 33.464983
    val row = 5 // 목록 수
    val key = "85pxiQNHO6gsxSjBFQfzN5lxPOIub30SlkWNkEvKSFjX%2BBl0sCbOltv6etE002jZB5OQkf9LFYqVZgpr2%2FivQg%3D%3D&"

    val url = "http://apis.data.go.kr/B552657/AEDInfoInqireService/getAedLcinfoInqire?WGS84_LON=$lon&WGS84_LAT=$lat&numOfRows=$row&serviceKey=$key"

    try {
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
        val itemList = xml.getElementsByTagName("item")
        for (i in 0 until itemList.length) {
            val n: Node = itemList.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) {
                val element = n as Element
                val map = mutableMapOf<String, String>()
                for (j in 0 until element.attributes.length) {
                    map.putIfAbsent(
                        element.attributes.item(j).nodeName,
                        element.attributes.item(j).nodeValue
                    )
                }

                place = element.getElementsByTagName("buildPlace").item(0).textContent
                org = element.getElementsByTagName("org").item(0).textContent

                println("< 이름 : $org , 설치 장소 : $place >")
            }
        }
    } catch(e: Exception) {
        e.printStackTrace()
    }
}