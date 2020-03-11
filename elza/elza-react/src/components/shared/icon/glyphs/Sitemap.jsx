import React from 'react';

const Sitemap = (props) => {
    return(
        <svg {...props}>
              <rect y="6.62" x="9.12" height="7.2" width="1.72"/>
              <path
                 d="M 1.88,13 3.6,12.9 c 0,-2.6 0.58,-2.2 6.2,-2.2 5.6,0 6.5,-0.5 6.5,2.1 h 1.8 C 18.1,8.47 16.3,8.95 9.8,8.95 3.35,8.95 1.88,8.3 1.88,13 Z"
                 />
              <rect width="5.03" height="6.38" x="7.44" y="1.23" ry="1.02" />
              <rect ry="1.02" y="12.4" x="7.44" height="6.38" width="5.03" />
              <rect width="5.03" height="6.38" x="0.288" y="12.4" ry="1.02" />
              <rect ry="1.02" y="12.4" x="14.7" height="6.38" width="5.03" />
        </svg>
    )
}
export default Sitemap;
