function StaticRating({ rating }) {
  return (
    <span>
      {[1, 2, 3, 4, 5].map(n => (
        <span key={n} style={{
          fontSize: '18px',
          filter: n <= rating ? 'none' : 'grayscale(1)',
          opacity: n <= rating ? 1 : 0.25,
        }}>🎬</span>
      ))}
    </span>
  )
}

export default StaticRating
