import { useState } from 'react'
import heroImg from './assets/hero.png'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import './App.css'

function App() {
  const [keyword, setKeyword] = useState("")
  const [videoUrl, setVideoUrl] = useState("")
  const[video, setVideo] = useState(null)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

    return (
    <div className="app">
        <header className = "appHeader">
        <strong className={"brand"}> YT/ Roullete <span className={"redDot"}></span>
        </strong>
            <div className={"headerRight"}>
                <span>GO FOR SOMETHING NEW</span>
                <small> Made by Ali Asgarov </small>
            </div>
        </header>

        <h1>SEARCH. ROLL. DISCOVER.</h1>
        <p className= "tagline"> Your next favorite video might be one click away.</p>
        <input type={"text"}
               placeholder={"Enter a theme of what u are searching for"}
               value = {keyword}
               onChange={(event) => setKeyword(event.target.value)} />
        <button onClick = {() => { if (keyword.trim() === ""){
            setError("Please enter a theme of what u are searching for");
            setVideo(null)
            return; }
            setVideo(null);
            setError("")
            setLoading(true);

            fetch (`http://localhost:8080/videos/random?keyword=${encodeURIComponent(keyword)}`)
            .then(response => { if(!response.ok){throw new Error("Video search failed!");}return response.json();})
            .then(data => { setVideo(data); setError("");})
            .catch(error => {setError(error.message); setVideo(null)})
            .finally (() => setLoading(false) ); }}
            disabled={loading }
        >
            {loading ? "searching " : "find random video"}
             </button>
        {error && <p>{error}</p>}
        {video && (
            <div className={"videoCard"}>
                <a href = {video.videoUrl}>
                <img src ={video.videoThumbnailUrl}
                               alt = {video.videoTitle} />  </a>
                <h2> {video.videoTitle}</h2>
                <p> {video.channelTitle }</p>
                <p> {video.videoDescription }</p>
                <a href = {video.videoUrl}> watch a random vid </a>

            </div>
        )}

    </div>);

}

export default App
